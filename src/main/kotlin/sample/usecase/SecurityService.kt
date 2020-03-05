package sample.usecase

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UsernameNotFoundException
import sample.ErrorKeys
import sample.context.security.ActorDetails
import sample.context.security.SecurityAdminService
import sample.context.security.SecurityUserService
import sample.usecase.admin.MasterAdminService
import sample.util.ConvertUtils
import java.util.*


/**
 * SpringSecurityのユーザアクセスコンポーネントを定義します。
 */
@Configuration
class SecurityService {

    /** 一般利用者情報を提供します。(see SecurityActorFinder)  */
    @Bean
    fun securityUserService(service: AccountService): SecurityUserService =
        object : SecurityUserService {
            /**
             * 以下の手順で利用口座を特定します。
             *
             * ログインID(全角は半角に自動変換)に合致するログイン情報があるか
             * 口座IDに合致する有効な口座情報があるか
             *
             * 一般利用者には「ROLE_USER」の権限が自動で割り当てられます。
             */
            override fun loadUserByUsername(username: String): ActorDetails =
                    Optional.ofNullable(username)
                            .map { ConvertUtils.zenkakuToHan(it)!! }
                            .flatMap {
                                service.getLoginByLoginId(it).flatMap { login ->
                                    // account to actorDetails
                                    service.getAccount(login.id!!).map { account ->
                                        val authorities = listOf(SimpleGrantedAuthority("ROLE_USER"))
                                        ActorDetails(account.actor(), login.password, authorities)
                                    }
                                }
                            }.orElseThrow { UsernameNotFoundException(ErrorKeys.Login) }
        }

    /** 社内管理向けの利用者情報を提供します。(see SecurityActorFinder)  */
    @Bean
    fun securityAdminService(service: MasterAdminService): SecurityAdminService =
        object : SecurityAdminService {
            /**
             * 以下の手順で社員を特定します。
             *
             * 社員ID(全角は半角に自動変換)に合致する社員情報があるか
             * 社員情報に紐付く権限があるか
             *
             * 社員には「ROLE_ADMIN」の権限が自動で割り当てられます。
             */
            override fun loadUserByUsername(username: String): ActorDetails =
                    Optional.ofNullable(username)
                            .map { ConvertUtils.zenkakuToHan(it)!! }
                            .flatMap { staffId ->
                                // staff to actorDetails
                                service.getStaff(staffId).map { staff ->
                                    val authorities = mutableListOf(SimpleGrantedAuthority("ROLE_ADMIN"))
                                    service.findStaffAuthority(staffId).forEach { authorities.add(SimpleGrantedAuthority(it.authority)) }
                                    ActorDetails(staff.actor(), staff.password, authorities)
                                }
                            }.orElseThrow({ UsernameNotFoundException(ErrorKeys.Login) })
        }

}