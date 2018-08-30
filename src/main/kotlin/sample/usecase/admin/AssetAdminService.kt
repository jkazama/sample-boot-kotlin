package sample.usecase.admin

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Transactional
import sample.context.DomainHelper
import sample.context.audit.AuditHandler
import sample.context.lock.IdLockHandler
import sample.context.lock.LockType
import sample.context.orm.DefaultRepository
import sample.model.asset.CashInOut
import sample.model.asset.Cashflow
import sample.model.asset.FindCashInOut
import sample.usecase.ServiceUtils

/**
 * 資産ドメインに対する社内ユースケース処理。
 */
@Service
class AssetAdminService(
        private val dh: DomainHelper,
        private val rep: DefaultRepository,
        @Qualifier(DefaultRepository.BeanNameTx)
        private val txm: PlatformTransactionManager,
        private val audit: AuditHandler,
        private val idLock: IdLockHandler
) {

    /**
     * 振込入出金依頼を検索します。
     * low: 口座横断的なので割り切りでREADロックはかけません。
     */
    @Transactional(DefaultRepository.BeanNameTx)
    fun findCashInOut(p: FindCashInOut): List<CashInOut> =
            CashInOut.find(rep, p)

    /**
     * 振込出金依頼を締めます。
     */
    fun closingCashOut() =
            audit.audit("振込出金依頼の締め処理をする") {
                ServiceUtils.tx(txm) { closingCashOutInTx() }
            }

    private fun closingCashOutInTx() =
    //low: 以降の処理は口座単位でfilter束ねしてから実行する方が望ましい。
    //low: 大量件数の処理が必要な時はそのままやるとヒープが死ぬため、idソートでページング分割して差分実行していく。
            CashInOut.findUnprocessed(rep).forEach { cio ->
                //low: TX内のロックが適切に動くかはIdLockHandlerの実装次第。
                // 調整が難しいようなら大人しく営業停止時間(IdLock必要な処理のみ非活性化されている状態)を作って、
                // ロック無しで一気に処理してしまう方がシンプル。
                idLock.call(cio.accountId, LockType.Write) {
                    try {
                        cio.process(rep)
                        //low: SQLの発行担保。扱う情報に相互依存が無く、セッションキャッシュはリークしがちなので都度消しておく。
                        rep.flushAndClear()
                    } catch (e: Exception) {
                        log.error("[" + cio.id + "] 振込出金依頼の締め処理に失敗しました。", e)
                        try {
                            cio.error(rep)
                            rep.flush()
                        } catch (ex: Exception) {
                            //low: 2重障害(恐らくDB起因)なのでloggerのみの記載に留める
                        }

                    }
                }
            }

    /**
     * キャッシュフローを実現します。
     *
     * 受渡日を迎えたキャッシュフローを残高に反映します。
     */
    fun realizeCashflow() =
            audit.audit("キャッシュフローを実現する") {
                ServiceUtils.tx(txm) { realizeCashflowInTx() }
            }

    private fun realizeCashflowInTx() {
        //low: 日回し後の実行を想定
        val day = dh.time.day()
        Cashflow.findDoRealize(rep, day).forEach { cf ->
            idLock.call(cf.accountId, LockType.Write) {
                try {
                    cf.realize(rep)
                    rep.flushAndClear()
                } catch (e: Exception) {
                    log.error("[" + cf.id + "] キャッシュフローの実現に失敗しました。", e)
                    try {
                        cf.error(rep)
                        rep.flush()
                    } catch (ex: Exception) {
                    }
                }
            }
        }
    }

    companion object {
        val log = LoggerFactory.getLogger(AssetAdminService::class.java)!!
    }

}