package sample

import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletRegistrationBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.MessageSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.core.annotation.Order
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter
import sample.context.actor.ActorSession
import sample.context.security.*


/**
 * アプリケーションのセキュリティ定義を表現します。
 */
@Configuration
@EnableConfigurationProperties(SecurityProperties::class)
class ApplicationSecurityConfig {

    /**
     * パスワード用のハッシュ(BCrypt)エンコーダー。
     * low: きちんとやるのであれば、strengthやSecureRandom使うなど外部切り出し含めて検討してください
     */
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    /** CORS全体適用  */
    @Bean
    @ConditionalOnProperty(prefix = "extension.security.cors", name = arrayOf("enabled"), matchIfMissing = false)
    fun corsFilter(props: SecurityProperties): CorsFilter {
        val source = UrlBasedCorsConfigurationSource()
        val config = CorsConfiguration()
        config.allowCredentials = props.cors.allowCredentials
        config.addAllowedOrigin(props.cors.allowedOrigin)
        config.addAllowedHeader(props.cors.allowedHeader)
        config.addAllowedMethod(props.cors.allowedMethod)
        config.maxAge = props.cors.maxAge
        source.registerCorsConfiguration(props.cors.path, config)
        return CorsFilter(source)
    }

    /** Spring Security を用いた API 認証/認可定義を表現します。  */
    @Configuration
    @EnableWebSecurity
    @EnableGlobalMethodSecurity(prePostEnabled = true, proxyTargetClass = true)
    @Import(SecurityConfigurer::class, SecurityProvider::class, SecurityEntryPoint::class, LoginHandler::class, SecurityActorFinder::class)
    @Order(org.springframework.boot.autoconfigure.security.SecurityProperties.BASIC_AUTH_ORDER)
    internal class AuthSecurityConfig {

        /** Spring Security のカスタム認証プロセス管理コンポーネント。  */
        @Bean
        fun authenticationManager(securityConfigurer: SecurityConfigurer): AuthenticationManager =
                securityConfigurer.authenticationManagerBean()

    }

}

