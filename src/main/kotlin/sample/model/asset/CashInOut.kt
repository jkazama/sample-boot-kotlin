package sample.model.asset

import sample.ActionStatusType
import sample.ErrorKeys
import sample.context.Dto
import sample.context.orm.JpqlBuilder
import sample.context.orm.OrmActiveMetaRecord
import sample.context.orm.OrmRepository
import sample.model.BusinessDayHandler
import sample.model.DomainErrorKeys
import sample.model.account.FiAccount
import sample.model.constraints.*
import sample.model.master.SelfFiAccount
import sample.util.DateUtils
import sample.util.TimePoint
import sample.util.Validator
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import javax.persistence.*
import javax.validation.constraints.NotNull


/**
 * 振込入出金依頼を表現するキャッシュフローアクション。
 *
 * 相手方/自社方の金融機関情報は依頼後に変更される可能性があるため、依頼時点の状態を
 * 保持するために非正規化して情報を保持しています。
 * low: 相手方/自社方の金融機関情報は項目数が多いのでサンプル用に金融機関コードのみにしています。
 * 実際の開発ではそれぞれ複合クラス(FinancialInstitution)に束ねるアプローチを推奨します。
 */
@Entity
data class CashInOut(
        /** ID(振込依頼No)  */
        @Id
        @GeneratedValue
        var id: Long? = null,
        /** 口座ID  */
        @IdStr
        val accountId: String,
        /** 通貨  */
        @Currency
        val currency: String,
        /** 金額(絶対値)  */
        @AbsAmount
        val absAmount: BigDecimal,
        /** 出金時はtrue  */
        val withdrawal: Boolean,
        /** 依頼日/日時  */
        @ISODate
        val requestDay: LocalDate,
        @ISODateTime
        val requestDate: LocalDateTime,
        /** 発生日  */
        @ISODate
        val eventDay: LocalDate,
        /** 受渡日  */
        @ISODate
        val valueDay: LocalDate,
        /** 相手方金融機関コード  */
        @IdStr
        val targetFiCode: String,
        /** 相手方金融機関口座ID  */
        @IdStr
        val targetFiAccountId: String,
        /** 自社方金融機関コード  */
        @IdStr
        val selfFiCode: String,
        /** 自社方金融機関口座ID  */
        @IdStr
        val selfFiAccountId: String,
        /** 処理種別  */
        @NotNull
        @Enumerated(EnumType.STRING)
        var statusType: ActionStatusType,
        /** キャッシュフローID。処理済のケースでのみ設定されます。low: 実際は調整CFや消込CFの概念なども有  */
        var cashflowId: Long? = null,
        /** 登録日時  */
        @ISODateTime
        override var createDate: LocalDateTime? = null,
        /** 登録者ID  */
        @IdStr
        override var createId: String? = null,
        /** 更新日時  */
        @ISODateTime
        override var updateDate: LocalDateTime? = null,
        /** 更新者ID  */
        @IdStr
        override var updateId: String? = null
) : OrmActiveMetaRecord<CashInOut>() {

    /**
     * 依頼を処理します。
     *
     * 依頼情報を処理済にしてキャッシュフローを生成します。
     */
    fun process(rep: OrmRepository): CashInOut {
        //low: 出金営業日の取得。ここでは単純な営業日を取得
        val now = rep.dh().time.tp()
        // 事前審査
        validate { v ->
            v.verify(statusType.isUnprocessed, ErrorKeys.ActionUnprocessing)
            v.verify(now.afterEqualsDay(eventDay), AssetErrorKeys.CashInOutAfterEqualsDay)
        }
        // 処理済状態を反映
        this.statusType = ActionStatusType.Processed
        this.cashflowId = Cashflow.register(rep, regCf()).id
        return update(rep)
    }

    private fun regCf(): RegCashflow {
        val amount = if (withdrawal) absAmount.negate() else absAmount
        val cashflowType = if (withdrawal) CashflowType.CashOut else CashflowType.CashIn
        // low: 摘要はとりあえずシンプルに。実際はCashInOutへ用途フィールドをもたせた方が良い(生成元メソッドに応じて摘要を変える)
        val remark = if (withdrawal) Remarks.CashOut else Remarks.CashIn
        return RegCashflow(accountId, currency, amount, cashflowType, remark, eventDay, valueDay)
    }

    /**
     * 依頼を取消します。
     *
     * "処理済みでない"かつ"発生日を迎えていない"必要があります。
     */
    fun cancel(rep: OrmRepository): CashInOut {
        val now = rep.dh().time.tp()
        // 事前審査
        validate { v ->
            v.verify(statusType.isUnprocessing, ErrorKeys.ActionUnprocessing)
            v.verify(now.beforeDay(eventDay), AssetErrorKeys.CashInOutBeforeEqualsDay)
        }
        // 取消状態を反映
        this.statusType = ActionStatusType.Cancelled
        return update(rep)
    }

    /**
     * 依頼をエラー状態にします。
     *
     * 処理中に失敗した際に呼び出してください。
     * low: 実際はエラー事由などを引数に取って保持する
     */
    fun error(rep: OrmRepository): CashInOut {
        validate { v -> v.verify(statusType.isUnprocessed, ErrorKeys.ActionUnprocessing) }

        this.statusType = ActionStatusType.Error
        return update(rep)
    }

    companion object {
        private const val serialVersionUID = 1L

        /** 振込入出金依頼を返します。  */
        fun load(rep: OrmRepository, id: Long?): CashInOut =
                rep.load(CashInOut::class.java, id!!)

        /** 未処理の振込入出金依頼一覧を検索します。  low: criteriaベース実装例  */
        fun find(rep: OrmRepository, p: FindCashInOut): List<CashInOut> {
            // low: 通常であれば事前にfrom/toの期間チェックを入れる
            val jpql = JpqlBuilder.of("FROM CashInOut c")
                    .equal("c.currency", p.currency)
                    .`in`("c.statusType", p.statusTypes)
                    .between("c.updateDate", p.updFromDay.atStartOfDay(), DateUtils.dateTo(p.updToDay))
                    .orderBy("c.updateDate DESC")
            return rep.tmpl().find(jpql.build(), *jpql.args())
        }

        /** 当日発生で未処理の振込入出金一覧を検索します。  */
        fun findUnprocessed(rep: OrmRepository): List<CashInOut> =
                rep.tmpl().find("FROM CashInOut c WHERE c.eventDay=?1 AND c.statusType IN ?2 ORDER BY c.id",
                        rep.dh().time.day(), ActionStatusType.unprocessedTypes)

        /** 未処理の振込入出金一覧を検索します。(口座別)  */
        fun findUnprocessed(rep: OrmRepository, accountId: String, currency: String, withdrawal: Boolean): List<CashInOut> =
                rep.tmpl().find(
                        "FROM CashInOut c WHERE c.accountId=?1 AND c.currency=?2 AND c.withdrawal=?3 AND c.statusType IN ?4 ORDER BY c.id",
                        accountId, currency, withdrawal, ActionStatusType.unprocessedTypes)

        /** 未処理の振込入出金一覧を検索します。(口座別)  */
        fun findUnprocessed(rep: OrmRepository, accountId: String): List<CashInOut> =
                rep.tmpl().find(
                        "FROM CashInOut c WHERE c.accountId=?1 AND c.statusType IN ?2 ORDER BY c.updateDate DESC",
                        accountId, ActionStatusType.unprocessedTypes)

        /** 出金依頼をします。  */
        fun withdraw(rep: OrmRepository, day: BusinessDayHandler, p: RegCashOut): CashInOut {
            val dh = rep.dh()
            val now = dh.time.tp()
            // low: 発生日は締め時刻等の兼ね合いで営業日と異なるケースが多いため、別途DB管理される事が多い
            val eventDay = day.day()
            // low: 実際は各金融機関/通貨の休日を考慮しての T+N 算出が必要
            val valueDay = day.day(3)

            // 事前審査
            Validator.validate({ v ->
                v.verifyField(0 < p.absAmount.signum(), "absAmount", DomainErrorKeys.AbsAmountZero)
                val canWithdraw = Asset(p.accountId!!).canWithdraw(rep, p.currency, p.absAmount, valueDay)
                v.verifyField(canWithdraw, "absAmount", AssetErrorKeys.CashInOutWithdrawAmount)
            })

            // 出金依頼情報を登録
            val acc = FiAccount.load(rep, p.accountId!!, Remarks.CashOut, p.currency)
            val selfAcc = SelfFiAccount.load(rep, Remarks.CashOut, p.currency)
            return p.create(now, eventDay, valueDay, acc, selfAcc).save(rep)
        }
    }

}

/** 振込入出金依頼の検索パラメタ。 low: 通常は顧客視点/社内視点で利用条件が異なる  */
data class FindCashInOut(
        @CurrencyEmpty
        val currency: String? = null,
        val statusTypes: List<ActionStatusType> = listOf(),
        @ISODate
        val updFromDay: LocalDate,
        @ISODate
        val updToDay: LocalDate
) : Dto

/** 振込出金の依頼パラメタ。   */
data class RegCashOut(
        /** 処理タイミングでは値が入ることが保証されます(暗黙設定されるケース向けに null 初期化を許容） */
        @IdStrEmpty
        val accountId: String? = null,
        @Currency
        val currency: String,
        @AbsAmount
        val absAmount: BigDecimal
) : Dto {

    fun create(now: TimePoint, eventDay: LocalDate, valueDay: LocalDate, acc: FiAccount,
               selfAcc: SelfFiAccount): CashInOut =
            CashInOut(
                    accountId = accountId!!,
                    currency = currency,
                    absAmount = absAmount,
                    withdrawal = true,
                    requestDay = now.day,
                    requestDate = now.date,
                    eventDay = eventDay,
                    valueDay = valueDay,
                    targetFiCode = acc.fiCode,
                    targetFiAccountId = acc.fiAccountId,
                    selfFiCode = selfAcc.fiCode,
                    selfFiAccountId = selfAcc.fiAccountId,
                    statusType = ActionStatusType.Unprocessed
            )

}
