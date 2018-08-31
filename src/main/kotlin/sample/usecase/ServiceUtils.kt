package sample.usecase

import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import sample.ErrorKeys
import sample.InvocationException
import sample.ValidationException
import sample.context.DomainHelper
import sample.context.actor.Actor

/**
 * Serviceで利用されるユーティリティ処理。
 */
object ServiceUtils {

    /** トランザクション処理を実行します。  */
    fun <T> tx(txm: PlatformTransactionManager, callable: () -> T): T =
            TransactionTemplate(txm).execute<T> {
                try {
                    callable()
                } catch (e: RuntimeException) {
                    throw e
                } catch (e: Exception) {
                    throw InvocationException("error.Exception", e);
                }
            }!!

    /** 匿名以外の利用者情報を返します。  */
    fun actorUser(dh: DomainHelper): Actor {
        val actor = dh.actor()
        if (actor.roleType.isAnonymous) {
            throw ValidationException(ErrorKeys.Authentication)
        }
        return actor
    }

}
