package sample.usecase

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import sample.context.DomainHelper
import sample.context.actor.Actor
import sample.context.audit.AuditHandler
import sample.context.lock.IdLockHandler
import sample.context.lock.LockType
import sample.context.orm.DefaultRepository
import sample.model.BusinessDayHandler
import sample.model.asset.CashInOut
import sample.model.asset.RegCashOut
import sample.usecase.mail.ServiceMailDeliver


/**
 * 資産ドメインに対する顧客ユースケース処理。
 */
@Service
class AssetService(
        private val dh: DomainHelper,
        private val rep: DefaultRepository,
        @Qualifier(DefaultRepository.BeanNameTx)
        private val txm: PlatformTransactionManager,
        private val audit: AuditHandler,
        private val idLock: IdLockHandler,
        private val businessDay: BusinessDayHandler,
        private val mail: ServiceMailDeliver
) {

    /** 匿名を除くActorを返します。  */
    private fun actor(): Actor = ServiceUtils.actorUser(dh)

    /**
     * 未処理の振込依頼情報を検索します。
     * low: 参照系は口座ロックが必要無いケースであれば@Transactionalでも十分
     * low: CashInOutは情報過多ですがアプリケーション層では公開対象を特定しにくい事もあり、
     * UI層に最終判断を委ねています。
     */
    fun findUnprocessedCashOut(): List<CashInOut> =
            idLock.call(actor().id, LockType.Read) {
                ServiceUtils.tx(txm) { CashInOut.findUnprocessed(rep, actor().id) }
            }

    /**
     * 振込出金依頼をします。
     * low: 公開リスクがあるためUI層には必要以上の情報を返さない事を意識します。
     * low: 監査ログの記録は状態を変えうる更新系ユースケースでのみ行います。
     * low: ロールバック発生時にメールが飛ばないようにトランザクション境界線を明確に分離します。
     * @return 振込出金依頼ID
     */
    fun withdraw(p: RegCashOut): Long =
            audit.audit("振込出金依頼をします") {
                val accId = actor().id
                val myParam = p.copy(accountId = accId) // 顧客側はログイン利用者で強制上書き
                // low: 口座IDロック(WRITE)とトランザクションをかけて振込処理
                val cio = idLock.call(accId, LockType.Read) {
                    ServiceUtils.tx(txm) { CashInOut.withdraw(rep, businessDay, myParam) }
                }
                // low: トランザクション確定後に出金依頼を受付した事をメール通知します。
                mail.sendWithdrawal(cio)
                cio.id!!
            }
}