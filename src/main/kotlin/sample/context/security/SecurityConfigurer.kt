package sample.context.security

import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletRegistrationBean
import org.springframework.context.MessageSource
import org.springframework.context.annotation.Lazy
import org.springframework.core.annotation.Order
import org.springframework.http.MediaType
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.InsufficientAuthenticationException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.builders.WebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.authentication.logout.LogoutFilter
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler
import org.springframework.stereotype.Component
import org.springframework.web.filter.CorsFilter
import org.springframework.web.filter.GenericFilterBean
import sample.ErrorKeys
import sample.context.actor.ActorSession
import java.io.IOException
import java.util.*
import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


/**
 * Spring Security(認証/認可)全般の設定を行います。
 *
 * 認証はベーシック認証ではなく、HttpSessionを用いた従来型のアプローチで定義しています。
 *
 * 設定はパターンを決め打ちしている関係上、既存の定義ファイルをラップしています。
 * securityプリフィックスではなくextension.securityプリフィックスのものを利用してください。
 *
 * low: HttpSessionを利用しているため、横スケールする際に問題となります。その際は上位のL/Bで制御するか、
 * SpringSession(HttpSessionの実装をRedis等でサポート)を利用する事でコード変更無しに対応が可能となります。
 *
 * low: 本サンプルでは無効化していますが、CSRF対応はプロジェクト毎に適切な利用を検討してください。
 */
@Component
@Order(org.springframework.boot.autoconfigure.security.SecurityProperties.BASIC_AUTH_ORDER)
class SecurityConfigurer(
        /** 拡張セキュリティ情報  */
        private val props: SecurityProperties,
        /** カスタムエントリポイント(例外対応)  */
        @Lazy
        private val entryPoint: SecurityEntryPoint,
        /** ログイン/ログアウト時の拡張ハンドラ  */
        @Lazy
        private val loginHandler: LoginHandler,
        /** ThreadLocalスコープの利用者セッション  */
        @Lazy
        private val actorSession: ActorSession,
        /** CORS利用時のフィルタ  */
        private val corsFilter: ObjectProvider<CorsFilter>,
        /** 認証配下に置くServletFilter  */
        private val filters: ObjectProvider<SecurityFilters>,
        /** 適用対象となる DistpatcherServlet 登録情報  */
        @Qualifier(DispatcherServletAutoConfiguration.DEFAULT_DISPATCHER_SERVLET_REGISTRATION_BEAN_NAME)
        private val dispatcherServletRegistration: DispatcherServletRegistrationBean
) : WebSecurityConfigurerAdapter() {

    override fun configure(web: WebSecurity?) {
        web!!.ignoring().mvcMatchers(
                *props.auth.ignorePath.map { dispatcherServletRegistration.getRelativePath(it) }.toTypedArray())
    }

    override fun configure(http: HttpSecurity) {
        // Target URL
        if (props.auth.enabled) {
            http
                    .authorizeRequests()
                    .mvcMatchers(*props.auth.excludesPath.toTypedArray()).permitAll()
            http
                    .csrf().disable()
                    .authorizeRequests()
                    .mvcMatchers(*props.auth.pathAdmin.toTypedArray()).hasRole("ADMIN")
                    .mvcMatchers(*props.auth.path.toTypedArray()).hasRole("USER")
        } else {
            // 認証無効時は全てのリクエストを許容
            http
                    .csrf().disable()
                    .authorizeRequests()
                    .mvcMatchers("/**").permitAll()
        }

        // Common
        http
                .exceptionHandling().authenticationEntryPoint(entryPoint)
        http
                .sessionManagement()
                .maximumSessions(props.auth.maximumSessions)
                .and()
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
        http
                .addFilterAfter(ActorSessionFilter(actorSession), UsernamePasswordAuthenticationFilter::class.java)
        corsFilter.ifAvailable { http.addFilterBefore(it, LogoutFilter::class.java) }
        filters.ifAvailable { filters ->
            filters.filters().forEach {
                http.addFilterAfter(it, ActorSessionFilter::class.java)
            }
        }

        // login/logout
        http
                .formLogin().loginPage(props.auth.loginPath)
                .usernameParameter(props.auth.loginKey).passwordParameter(props.auth.passwordKey)
                .successHandler(loginHandler).failureHandler(loginHandler)
                .permitAll()
                .and()
                .logout().logoutUrl(props.auth.logoutPath)
                .logoutSuccessHandler(loginHandler)
                .permitAll()
    }

}

/**
 * Spring Securityのカスタム認証プロバイダ。
 *
 * 主にパスワード照合を行っています。
 */
@Component
class SecurityProvider(
        private val actorFinder: SecurityActorFinder,
        private val encoder: PasswordEncoder
) : AuthenticationProvider {

    override fun authenticate(authentication: Authentication): Authentication {
        if (authentication.principal == null || authentication.credentials == null) {
            throw BadCredentialsException("ログイン認証に失敗しました")
        }
        val service = actorFinder.detailsService()
        val details = service.loadUserByUsername(authentication.principal.toString())
        val presentedPassword = authentication.credentials.toString()
        if (!encoder.matches(presentedPassword, details.password)) {
            throw BadCredentialsException("ログイン認証に失敗しました")
        }
        val ret = UsernamePasswordAuthenticationToken(
                authentication.name, "", details.authorities)
        ret.details = details
        return ret
    }

    override fun supports(authentication: Class<*>): Boolean {
        return UsernamePasswordAuthenticationToken::class.java.isAssignableFrom(authentication)
    }
}

/**
 * Spring Securityのカスタムエントリポイント。
 *
 * API化を念頭に例外発生時の実装差込をしています。
 */
@Component
class SecurityEntryPoint(
        private val msg: MessageSource
) : AuthenticationEntryPoint {

    override fun commence(
            request: HttpServletRequest, response: HttpServletResponse, authException: AuthenticationException) {
        if (response.isCommitted) {
            return
        }
        if (authException is InsufficientAuthenticationException) {
            val message = msg.getMessage(ErrorKeys.AccessDenied, arrayOfNulls(0), Locale.getDefault())
            writeResponseEmpty(response, HttpServletResponse.SC_FORBIDDEN, message)
        } else {
            val message = msg.getMessage(ErrorKeys.Authentication, arrayOfNulls(0), Locale.getDefault())
            writeResponseEmpty(response, HttpServletResponse.SC_UNAUTHORIZED, message)
        }
    }

    private fun writeResponseEmpty(response: HttpServletResponse, status: Int, message: String) {
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.status = status
        response.characterEncoding = "UTF-8"
        response.writer.write("{\"message\": \"$message\"}")
    }
}

/**
 * SpringSecurityの認証情報(Authentication)とActorSessionを紐付けるServletFilter。
 *
 * dummyLoginが有効な時は常にSecurityContextHolderへAuthenticationを紐付けます。
 */
@Component
class ActorSessionFilter(
        private val actorSession: ActorSession
) : GenericFilterBean() {

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val authOpt = SecurityActorFinder.authentication()
        if (authOpt.isPresent && authOpt.get().details is ActorDetails) {
            val details = authOpt.get().details as ActorDetails
            actorSession.bind(details.actor)
            try {
                chain.doFilter(request, response)
            } finally {
                actorSession.unbind()
            }
        } else {
            actorSession.unbind()
            chain.doFilter(request, response)
        }
    }
}

/**
 * Spring Securityにおけるログイン/ログアウト時の振る舞いを拡張するHandler。
 */
@Component
class LoginHandler() : AuthenticationSuccessHandler, AuthenticationFailureHandler, LogoutSuccessHandler {

    /** ログイン成功処理  */
    override fun onAuthenticationSuccess(
            request: HttpServletRequest, response: HttpServletResponse, authentication: Authentication) {
        Optional.ofNullable(authentication.details as ActorDetails).ifPresent { it.bindRequestInfo(request) }
        if (response.isCommitted) {
            return
        }
        writeResponseEmpty(response, HttpServletResponse.SC_OK)
    }

    /** ログイン失敗処理  */
    override fun onAuthenticationFailure(
            request: HttpServletRequest, response: HttpServletResponse, exception: AuthenticationException) {
        if (response.isCommitted) {
            return
        }
        writeResponseEmpty(response, HttpServletResponse.SC_BAD_REQUEST)
    }

    /** ログアウト成功処理  */
    override fun onLogoutSuccess(
            request: HttpServletRequest, response: HttpServletResponse, authentication: Authentication) {
        if (response.isCommitted) {
            return
        }
        writeResponseEmpty(response, HttpServletResponse.SC_OK)
    }

    private fun writeResponseEmpty(response: HttpServletResponse, status: Int) {
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.status = status
        response.writer.write("{}")
    }
}
