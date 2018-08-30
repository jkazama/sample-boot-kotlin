package sample.controller

import org.aspectj.lang.annotation.After
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import sample.context.actor.ActorRoleType
import sample.context.actor.Actor
import sample.context.actor.ActorSession
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import sample.context.security.SecurityConfigurer


/**
 * Spring Securityの設定状況に応じてスレッドローカルへ利用者を紐付けるAOPInterceptor。
 */
@Aspect
@Configuration
class LoginInterceptor(private val session: ActorSession) {

    @Before("execution(* *..controller.system.*Controller.*(..))")
    fun bindSystem() {
        session.bind(Actor.System)
    }

    @After("execution(* *..controller..*Controller.*(..))")
    fun unbind() {
        session.unbind()
    }

    /**
     * セキュリティの認証設定(extension.security.auth.enabled)が無効時のみ有効される擬似ログイン処理。
     *
     * 開発時のみ利用してください。
     */
    @Aspect
    @Component
    @ConditionalOnProperty(name = ["extension.security.auth.enabled"], havingValue = "false", matchIfMissing = true)
    class DummyLoginInterceptor(private val session: ActorSession) {

        @Before("execution(* *..controller.*Controller.*(..))")
        fun bindUser() {
            session.bind(Actor("sample", ActorRoleType.User))
        }

        @Before("execution(* *..controller.admin.*Controller.*(..))")
        fun bindAdmin() {
            session.bind(Actor("admin", ActorRoleType.Internal))
        }
    }

}
