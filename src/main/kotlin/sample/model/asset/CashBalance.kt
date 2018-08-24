package sample.model.asset

import sample.context.orm.OrmActiveRecord
import sample.context.orm.OrmRepository
import sample.model.constraints.*
import sample.util.Calculator
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id

/**
 * 口座残高を表現します。
 */
@Entity
class CashBalance(
        /** ID  */
        @Id
        @GeneratedValue
        var id: Long? = null,
        /** 口座ID  */
        @IdStr
        val accountId: String,
        /** 基準日  */
        @ISODate
        val baseDay: LocalDate,
        /** 通貨  */
        @Currency
        val currency: String,
        /** 金額  */
        @Amount
        var amount: BigDecimal,
        /** 更新日  */
        @ISODateTime
        var updateDate: LocalDateTime
) : OrmActiveRecord<CashBalance>() {

    /**
     * 残高へ指定した金額を反映します。
     * low ここではCurrencyを使っていますが、実際の通貨桁数や端数処理定義はDBや設定ファイル等で管理されます。
     */
    fun add(rep: OrmRepository, addAmount: BigDecimal): CashBalance {
        val scale = java.util.Currency.getInstance(currency).defaultFractionDigits
        this.amount = Calculator.of(amount).scale(scale, RoundingMode.DOWN)
                .add(addAmount)
                .decimal
        return update(rep)
    }

    companion object {
        private const val serialVersionUID = 1L

        /**
         * 指定口座の残高を取得します。(存在しない時は繰越保存後に取得します)
         * low: 複数通貨の適切な考慮や細かい審査は本筋でないので割愛。
         */
        fun getOrNew(rep: OrmRepository, accountId: String, currency: String): CashBalance {
            val baseDay = rep.dh().time.day()
            val m = rep.tmpl().get<CashBalance>(
                    "FROM CashBalance c WHERE c.accountId=?1 AND c.currency=?2 AND c.baseDay=?3 ORDER BY c.baseDay DESC",
                    accountId, currency, baseDay)
            return m.orElseGet { create(rep, accountId, currency) }
        }

        private fun create(rep: OrmRepository, accountId: String, currency: String): CashBalance {
            val now = rep.dh().time.tp()
            val m = rep.tmpl().get<CashBalance>(
                    "FROM CashBalance c WHERE c.accountId=?1 AND c.currency=?2 ORDER BY c.baseDay DESC",
                    accountId, currency)
            if (m.isPresent) { // 残高繰越
                val prev = m.get()
                return CashBalance(accountId = accountId, baseDay = now.day, currency = currency, amount = prev.amount, updateDate = now.date).save(rep)
            } else {
                return CashBalance(accountId = accountId, baseDay = now.day, currency = currency, amount = BigDecimal.ZERO, updateDate = now.date).save(rep)
            }
        }
    }

}
