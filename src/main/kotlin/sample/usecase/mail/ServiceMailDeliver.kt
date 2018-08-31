package sample.usecase.mail

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import sample.context.mail.MailHandler
import sample.context.orm.DefaultRepository
import sample.model.asset.CashInOut
import sample.usecase.ServiceUtils


/**
 * アプリケーション層のサービスメール送信を行います。
 * <p>独自にトランザクションを管理するので、サービスのトランザクション内で
 * 呼び出さないように注意してください。
 */
@Component
class ServiceMailDeliver(
        private val rep: DefaultRepository,
        @Qualifier(DefaultRepository.BeanNameTx)
        private val txm: PlatformTransactionManager,
        private val mail: MailHandler
) {
        /** トランザクション処理を実行します。  */
        private fun <T> tx(callable: () -> T): T = ServiceUtils.tx(txm, callable)

        /** 出金依頼受付メールを送信します。  */
        fun sendWithdrawal(cio: CashInOut) {
                //low: サンプルなので未実装。実際は独自にトランザクションを貼って処理を行う
        }

}