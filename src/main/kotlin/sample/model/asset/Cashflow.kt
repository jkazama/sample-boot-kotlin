package sample.model.asset

import org.springframework.format.annotation.DateTimeFormat
import sample.ActionStatusType
import sample.ErrorKeys
import sample.context.Dto
import sample.context.orm.OrmActiveMetaRecord
import sample.context.orm.OrmRepository
import sample.model.constraints.*
import sample.util.TimePoint
import sample.util.Validator
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import javax.persistence.*
import javax.validation.constraints.NotNull

/**
 * 入出金キャッシュフローを表現します。
 * キャッシュフローは振込/振替といったキャッシュフローアクションから生成される確定状態(依頼取消等の無い)の入出金情報です。
 * low: 概念を伝えるだけなので必要最低限の項目で表現しています。
 * low: 検索関連は主に経理確認や帳票等での利用を想定します
 */
@Entity
data class Cashflow(
        /** ID  */
        @Id
        @GeneratedValue
        var id: Long? = null,
        /** 口座ID  */
        @field:IdStr
        val accountId: String,
        /** 通貨  */
        @field:Currency
        val currency: String,
        /** 金額  */
        @field:Amount
        val amount: BigDecimal,
        /** 入出金  */
        @field:NotNull
        @Enumerated(EnumType.STRING)
        val cashflowType: CashflowType,
        /** 摘要  */
        @field:Category
        val remark: String,
        /** 発生日/日時  */
        @field:NotNull
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        val eventDay: LocalDate,
        @field:NotNull
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        val eventDate: LocalDateTime,
        /** 受渡日  */
        @field:NotNull
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        val valueDay: LocalDate,
        /** 処理種別  */
        @field:NotNull
        @Enumerated(EnumType.STRING)
        var statusType: ActionStatusType,
        /** 登録日時  */
        @field:NotNull
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        override var createDate: LocalDateTime? = null,
        /** 登録者ID  */
        @field:IdStr
        override var createId: String? = null,
        /** 更新日時  */
        @field:NotNull
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        override var updateDate: LocalDateTime? = null,
        /** 更新者ID  */
        @field:IdStr
        override var updateId: String? = null
) : OrmActiveMetaRecord<Cashflow>() {

    /** キャッシュフローを処理済みにして残高へ反映します。  */
    fun realize(rep: OrmRepository): Cashflow {
        validate { v ->
            v.verify(this.canRealize(rep), AssetErrorKeys.CashflowRealizeDay)
            v.verify(this.statusType.isUnprocessing, ErrorKeys.ActionUnprocessing) // 「既に処理中/処理済です」
        }

        this.statusType = ActionStatusType.Processed
        update(rep)
        CashBalance.getOrNew(rep, accountId, currency).add(rep, amount)
        return this
    }

    /**
     * キャッシュフローをエラー状態にします。
     *
     * 処理中に失敗した際に呼び出してください。
     * low: 実際はエラー事由などを引数に取って保持する
     */
    fun error(rep: OrmRepository): Cashflow {
        validate { v ->
            v.verify(statusType.isUnprocessed, ErrorKeys.ActionUnprocessing)
        }

        this.statusType = ActionStatusType.Error
        return update(rep)
    }

    /** キャッシュフローを実現(受渡)可能か判定します。  */
    fun canRealize(rep: OrmRepository): Boolean =
            rep.dh().time.tp().afterEqualsDay(valueDay)

    companion object {
        private const val serialVersionUID = 1L

        /** キャッシュフローを取得します。(例外付)  */
        fun load(rep: OrmRepository, id: Long): Cashflow =
                rep.load(Cashflow::class.java, id)

        /**
         * 指定受渡日時点で未実現のキャッシュフロー一覧を検索します。(口座通貨別)
         */
        fun findUnrealize(rep: OrmRepository, accountId: String, currency: String, valueDay: LocalDate): List<Cashflow> =
                rep.tmpl().find(
                        "FROM Cashflow c WHERE c.accountId=?1 AND c.currency=?2 AND c.valueDay<=?3 AND c.statusType IN ?4 ORDER BY c.id",
                        accountId, currency, valueDay, ActionStatusType.unprocessingTypes)

        /**
         * 指定受渡日で実現対象となるキャッシュフロー一覧を検索します。
         */
        fun findDoRealize(rep: OrmRepository, valueDay: LocalDate): List<Cashflow> =
                rep.tmpl().find("FROM Cashflow c WHERE c.valueDay=?1 AND c.statusType IN ?2 ORDER BY c.id",
                        valueDay, ActionStatusType.unprocessedTypes)

        /**
         * キャッシュフローを登録します。
         * 受渡日を迎えていた時はそのまま残高へ反映します。
         */
        fun register(rep: OrmRepository, p: RegCashflow): Cashflow {
            val now = rep.dh().time.tp()
            Validator.validate { v ->
                v.checkField(now.beforeEqualsDay(p.valueDay!!),
                        "valueDay", AssetErrorKeys.CashflowBeforeEqualsDay)
            }
            val cf = p.create(now).save(rep)
            rep.flush() // ID発行保証
            return if (cf.canRealize(rep)) cf.realize(rep) else cf
        }
    }

}

/** キャッシュフロー種別。 low: 各社固有です。摘要含めラベルはなるべくmessages.propertiesへ切り出し  */
enum class CashflowType {
    /** 振込入金  */
    CashIn,
    /** 振込出金  */
    CashOut,
    /** 振替入金  */
    CashTransferIn,
    /** 振替出金  */
    CashTransferOut
}

/** 入出金キャッシュフローの登録パラメタ。   */
data class RegCashflow(
        @field:IdStr
        val accountId: String? = null,
        @field:Currency
        val currency: String? = null,
        @field:Amount
        val amount: BigDecimal? = null,
        @field:NotNull
        val cashflowType: CashflowType? = null,
        @field:Category
        val remark: String? = null,
        /** 未設定時は営業日を設定  */
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        val eventDay: LocalDate? = null,
        @field:NotNull
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        val valueDay: LocalDate? = null
) : Dto {
    fun create(now: TimePoint): Cashflow {
        val eventDate = if (eventDay == null) now else TimePoint(eventDay, now.date)
        return Cashflow(
                accountId = accountId!!,
                currency = currency!!,
                amount = amount!!,
                cashflowType = cashflowType!!,
                remark = remark!!,
                eventDay = eventDate.day,
                eventDate = eventDate.date,
                valueDay = valueDay!!,
                statusType = ActionStatusType.Unprocessed
        )
    }
}
