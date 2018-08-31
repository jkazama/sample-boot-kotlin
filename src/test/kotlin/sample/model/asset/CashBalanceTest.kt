package sample.model.asset

import org.hamcrest.Matchers.*
import org.junit.Assert.assertThat
import org.junit.Test
import sample.EntityTestSupport
import java.math.BigDecimal

//low: 簡易な正常系検証のみ
class CashBalanceTest : EntityTestSupport() {

    override fun setupPreset() {
        targetEntities(CashBalance::class.java)
    }

    @Test
    fun 現金残高を追加する() {
        val baseDay = businessDay.day()
        tx {
            val cb = fixtures().cb("test1", baseDay, "USD", "10.02").save(rep())

            // 10.02 + 11.51 = 21.53
            assertThat(cb.add(rep(), BigDecimal("11.51")).amount, `is`(BigDecimal("21.53")))

            // 21.53 + 11.516 = 33.04 (端数切捨確認)
            assertThat(cb.add(rep(), BigDecimal("11.516")).amount, `is`(BigDecimal("33.04")))

            // 33.04 - 41.51 = -8.47 (マイナス値/マイナス残許容)
            assertThat(cb.add(rep(), BigDecimal("-41.51")).amount, `is`(BigDecimal("-8.47")))
        }
    }

    @Test
    fun 現金残高を取得する() {
        val baseDay = businessDay.day()
        val baseMinus1Day = businessDay.day(-1)
        tx {
            fixtures().cb("test1", baseDay, "JPY", "1000").save(rep())
            fixtures().cb("test2", baseMinus1Day, "JPY", "3000").save(rep())

            // 存在している残高の検証
            val cbNormal = CashBalance.getOrNew(rep(), "test1", "JPY")
            assertThat(cbNormal, allOf(
                    hasProperty("accountId", `is`("test1")),
                    hasProperty("baseDay", `is`(baseDay)),
                    hasProperty("amount", `is`(BigDecimal("1000")))))

            // 基準日に存在していない残高の繰越検証
            val cbRoll = CashBalance.getOrNew(rep(), "test2", "JPY")
            assertThat(cbRoll, allOf(
                    hasProperty("accountId", `is`("test2")),
                    hasProperty("baseDay", `is`(baseDay)),
                    hasProperty("amount", `is`(BigDecimal("3000")))))

            // 残高を保有しない口座の生成検証
            val cbNew = CashBalance.getOrNew(rep(), "test3", "JPY")
            assertThat(cbNew, allOf(
                    hasProperty("accountId", `is`("test3")),
                    hasProperty("baseDay", `is`(baseDay)),
                    hasProperty("amount", `is`(BigDecimal.ZERO))))
        }
    }
}
