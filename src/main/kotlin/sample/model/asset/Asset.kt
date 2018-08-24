package sample.model.asset

import jdk.nashorn.internal.objects.NativeArray.forEach
import sample.util.Calculator
import java.time.LocalDate
import sample.context.orm.OrmRepository
import java.math.BigDecimal


/**
 * 口座の資産概念を表現します。
 * asset配下のEntityを横断的に取り扱います。
 * low: 実際の開発では多通貨や執行中/拘束中のキャッシュフローアクションに対する考慮で、サービスによってはかなり複雑になります。
 */
class Asset(val id: String) {
    /**
     * 振込出金可能か判定します。
     *
     * 0 &lt;= 口座残高 + 未実現キャッシュフロー - (出金依頼拘束額 + 出金依頼額)
     * low: 判定のみなのでscale指定は省略。余力金額を返す時はきちんと指定する
     */
    fun canWithdraw(rep: OrmRepository, currency: String, absAmount: BigDecimal, valueDay: LocalDate): Boolean {
        val calc = Calculator.of(CashBalance.getOrNew(rep, id, currency).amount)
        Cashflow.findUnrealize(rep, id, currency, valueDay).stream().forEach { calc.add(it.amount) }
        CashInOut.findUnprocessed(rep, id, currency, true).stream()
                .forEach { calc.add(it.absAmount.negate()) }
        calc.add(absAmount.negate())
        return 0 <= calc.decimal.signum()
    }
}