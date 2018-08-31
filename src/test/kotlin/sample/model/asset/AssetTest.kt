package sample.model.asset

import org.hamcrest.Matchers.`is`
import org.junit.Assert.assertThat
import org.junit.Test
import java.time.LocalDate
import sample.model.account.Account
import sample.EntityTestSupport
import java.math.BigDecimal

//low: 簡易な検証が中心
class AssetTest : EntityTestSupport() {

    override fun setupPreset() {
        targetEntities(Account::class.java, CashBalance::class.java, Cashflow::class.java, CashInOut::class.java)
    }

    @Test
    fun 振込出金可能か判定する() {
        // 残高   +  未実現キャッシュフロー - 出金依頼拘束額 = 出金可能額
        // 10000 + (1000 - 2000) - 8000 = 1000
        tx {
            fixtures().acc("test").save(rep())
            fixtures().cb("test", LocalDate.of(2014, 11, 18), "JPY", "10000").save(rep())
            fixtures().cf("test", "1000", LocalDate.of(2014, 11, 18), LocalDate.of(2014, 11, 20)).save(rep())
            fixtures().cf("test", "-2000", LocalDate.of(2014, 11, 19), LocalDate.of(2014, 11, 21)).save(rep())
            fixtures().cio("test", "8000", true).save(rep())

            assertThat(
                    Asset("test").canWithdraw(rep(), "JPY", BigDecimal("1000"), LocalDate.of(2014, 11, 21)),
                    `is`(true))
            assertThat(
                    Asset("test").canWithdraw(rep(), "JPY", BigDecimal("1001"), LocalDate.of(2014, 11, 21)),
                    `is`(false))
        }
    }

}
