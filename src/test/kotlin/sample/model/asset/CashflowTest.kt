package sample.model.asset

import org.hamcrest.Matchers.*
import org.junit.Assert.assertThat
import org.junit.Assert.fail
import org.junit.Test
import java.time.LocalDate
import sample.ActionStatusType
import sample.ErrorKeys
import sample.EntityTestSupport
import sample.ValidationException
import java.math.BigDecimal

//low: 簡易な正常系検証が中心。依存するCashBalanceの単体検証パスを前提。
class CashflowTest : EntityTestSupport() {

    override fun setupPreset() {
        targetEntities(Cashflow::class.java, CashBalance::class.java)
    }

    @Test
    fun キャッシュフローを登録する() {
        val baseDay = businessDay.day()
        val baseMinus1Day = businessDay.day(-1)
        val basePlus1Day = businessDay.day(1)
        tx {
            // 過去日付の受渡でキャッシュフロー発生 [例外]
            try {
                Cashflow.register(rep(), fixtures().cfReg("test1", "1000", baseMinus1Day))
                fail()
            } catch (e: ValidationException) {
                assertThat(e.message, `is`(AssetErrorKeys.CashflowBeforeEqualsDay))
            }

            // 翌日受渡でキャッシュフロー発生
            assertThat(Cashflow.register(rep(), fixtures().cfReg("test1", "1000", basePlus1Day)),
                    allOf(
                            hasProperty("amount", `is`(BigDecimal("1000"))),
                            hasProperty("statusType", `is`(ActionStatusType.Unprocessed)),
                            hasProperty("eventDay", `is`(baseDay)),
                            hasProperty("valueDay", `is`(basePlus1Day))))
        }
    }

    @Test
    fun 未実現キャッシュフローを実現する() {
        val baseDay = businessDay.day()
        val baseMinus1Day = businessDay.day(-1)
        val baseMinus2Day = businessDay.day(-2)
        val basePlus1Day = businessDay.day(1)
        tx {
            CashBalance.getOrNew(rep(), "test1", "JPY")

            // 未到来の受渡日 [例外]
            val cfFuture = fixtures().cf("test1", "1000", baseDay, basePlus1Day).save(rep())
            try {
                cfFuture.realize(rep())
                fail()
            } catch (e: ValidationException) {
                assertThat(e.message, `is`(AssetErrorKeys.CashflowRealizeDay))
            }

            // キャッシュフローの残高反映検証。  0 + 1000 = 1000
            val cfNormal = fixtures().cf("test1", "1000", baseMinus1Day, baseDay).save(rep())
            assertThat(cfNormal.realize(rep()), hasProperty("statusType", `is`(ActionStatusType.Processed)))
            assertThat(CashBalance.getOrNew(rep(), "test1", "JPY"),
                    hasProperty("amount", `is`(BigDecimal("1000"))))

            // 処理済キャッシュフローの再実現 [例外]
            try {
                cfNormal.realize(rep())
                fail()
            } catch (e: ValidationException) {
                assertThat(e.message, `is`(ErrorKeys.ActionUnprocessing))
            }

            // 過日キャッシュフローの残高反映検証。 1000 + 2000 = 3000
            val cfPast = fixtures().cf("test1", "2000", baseMinus2Day, baseMinus1Day).save(rep())
            assertThat(cfPast.realize(rep()), hasProperty("statusType", `is`(ActionStatusType.Processed)))
            assertThat(CashBalance.getOrNew(rep(), "test1", "JPY"),
                    hasProperty("amount", `is`(BigDecimal("3000"))))
        }
    }

    @Test
    fun 発生即実現のキャッシュフローを登録する() {
        val baseDay = businessDay.day()
        tx {
            CashBalance.getOrNew(rep(), "test1", "JPY")
            // 発生即実現
            Cashflow.register(rep(), fixtures().cfReg("test1", "1000", baseDay))
            assertThat(CashBalance.getOrNew(rep(), "test1", "JPY"),
                    hasProperty("amount", `is`(BigDecimal("1000"))))
        }
    }

}
