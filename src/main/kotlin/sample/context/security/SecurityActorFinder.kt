package sample.context.security

import org.springframework.beans.factory.ObjectProvider
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import sample.context.actor.Actor
import java.util.*
import java.util.stream.Collectors
import javax.servlet.http.HttpServletRequest

/**
 * Spring Securityで利用される認証/認可対象となるユーザ情報を提供します。
 */
class SecurityActorFinder(
        val props: SecurityProperties,
        val userService: ObjectProvider<SecurityUserService>,
        val adminService: ObjectProvider<SecurityAdminService>
) {

    /** 現在のプロセス状態に応じたUserDetailServiceを返します。  */
    fun detailsService(): SecurityActorService =
            if (props.auth.admin) adminService() else userService.getObject()

    private fun adminService(): SecurityAdminService =
            Optional.ofNullable(adminService.ifAvailable)
                    .orElseThrow({ IllegalStateException("SecurityAdminServiceをコンテナへBean登録してください。") })

    companion object {

        /**
         * 現在有効な認証情報を返します。
         */
        fun authentication(): Optional<Authentication> =
                Optional.ofNullable(SecurityContextHolder.getContext().authentication)

        /**
         * 現在有効な利用者認証情報を返します。
         *
         * ログイン中の利用者情報を取りたいときはこちらを利用してください。
         */
        fun actorDetails(): Optional<ActorDetails> =
                authentication()
                        .filter { it.getDetails() is ActorDetails }
                        .map { it.getDetails() as ActorDetails }
    }

}

/**
 * 認証/認可で用いられるユーザ情報。
 *
 * プロジェクト固有にカスタマイズしています。
 */
data class ActorDetails(
        /** ログイン中の利用者情報  */
        val actor: Actor,
        /** 認証パスワード(暗号化済)  */
        private val password: String,
        /** 利用者の所有権限一覧  */
        private val authorities: Collection<GrantedAuthority>) : UserDetails {

    val authorityIds: List<String> = authorities.map { it.authority }

    fun bindRequestInfo(request: HttpServletRequest): ActorDetails {
        //low: L/B経由をきちんと考えるならヘッダーもチェックすること
        actor.source = request.getRemoteAddr()
        return this
    }

    override fun getUsername(): String {
        return actor.id
    }

    override fun getPassword(): String {
        return password
    }

    override fun isAccountNonExpired(): Boolean {
        return true
    }

    override fun isAccountNonLocked(): Boolean {
        return true
    }

    override fun isCredentialsNonExpired(): Boolean {
        return true
    }

    override fun isEnabled(): Boolean {
        return true
    }

    override fun getAuthorities(): Collection<GrantedAuthority> {
        return authorities
    }

    companion object {
        private val serialVersionUID = 1L
    }

}

/** Actorに適合したUserDetailsService  */
interface SecurityActorService : UserDetailsService {
    /**
     * 与えられたログインIDを元に認証/認可対象のユーザ情報を返します。
     * @see org.springframework.security.core.userdetails.UserDetailsService.loadUserByUsername
     */
    @Throws(UsernameNotFoundException::class)
    override fun loadUserByUsername(username: String): ActorDetails
}

/** 一般利用者向けI/F  */
interface SecurityUserService : SecurityActorService

/** 管理者向けI/F  */
interface SecurityAdminService : SecurityActorService
