package sample.model.asset

import org.hamcrest.Matchers.*
import org.junit.Assert.assertThat
import org.junit.Assert.fail
import org.junit.Test
import sample.ActionStatusType
import sample.EntityTestSupport
import sample.ErrorKeys
import sample.ValidationException
import sample.model.DomainErrorKeys
import sample.model.account.Account
import sample.model.account.FiAccount
import sample.model.master.SelfFiAccount
import java.math.BigDecimal
import java.time.LocalDate


//low: 簡易な正常系検証が中心。依存するCashflow/CashBalanceの単体検証パスを前提。
class CashInOutTest : EntityTestSupport() {

    override fun setupPreset() {
        targetEntities(Account::class.java, FiAccount::class.java, SelfFiAccount::class.java,
                CashInOut::class.java, Cashflow::class.java, CashBalance::class.java)
    }

    override fun before() {
        // 残高1000円の口座(test)を用意
        val baseDay = businessDay.day()
        tx {
            fixtures().selfFiAcc(Remarks.CashOut, ccy).save(rep())
            fixtures().acc(accId).save(rep())
            fixtures().fiAcc(accId, Remarks.CashOut, ccy).save(rep())
            fixtures().cb(accId, baseDay, ccy, "1000").save(rep())
        }
    }

    @Test
    fun 振込入出金を検索する() {
        val baseDay = businessDay.day()
        val basePlus1Day = businessDay.day(1)
        val basePlus2Day = businessDay.day(2)
        tx {
            fixtures().cio(accId, "300", true).save(rep())
            //low: ちゃんとやると大変なので最低限の検証
            assertThat(
                    CashInOut.find(rep(), findParam(baseDay, basePlus1Day)),
                    hasSize(1))
            assertThat(
                    CashInOut.find(rep(), findParam(baseDay, basePlus1Day, ActionStatusType.Unprocessed)),
                    hasSize(1))
            assertThat(
                    CashInOut.find(rep(), findParam(baseDay, basePlus1Day, ActionStatusType.Processed)),
                    empty())
            assertThat(
                    CashInOut.find(rep(), findParam(basePlus1Day, basePlus2Day, ActionStatusType.Unprocessed)),
                    empty())
        }
    }

    private fun findParam(fromDay: LocalDate, toDay: LocalDate, vararg statusTypes: ActionStatusType): FindCashInOut {
        return FindCashInOut(ccy, statusTypes.toList(), fromDay, toDay)
    }

    @Test
    fun 振込出金依頼をする() {
        val baseDay = businessDay.day()
        val basePlus3Day = businessDay.day(3)
        tx {
            // 超過の出金依頼 [例外]
            try {
                CashInOut.withdraw(rep(), businessDay, RegCashOut(accId, ccy, BigDecimal("1001")))
                fail()
            } catch (e: ValidationException) {
                assertThat(e.message, `is`(AssetErrorKeys.CashInOutWithdrawAmount))
            }

            // 0円出金の出金依頼 [例外]
            try {
                CashInOut.withdraw(rep(), businessDay, RegCashOut(accId, ccy, BigDecimal.ZERO))
                fail()
            } catch (e: ValidationException) {
                assertThat(e.message, `is`(DomainErrorKeys.AbsAmountZero))
            }

            // 通常の出金依頼
            val normal = CashInOut.withdraw(rep(), businessDay, RegCashOut(accId, ccy, BigDecimal("300")))
            assertThat(normal, allOf(
                    hasProperty("accountId", `is`(accId)), hasProperty("currency", `is`(ccy)),
                    hasProperty("absAmount", `is`(BigDecimal(300))), hasProperty("withdrawal", `is`(true)),
                    hasProperty("requestDay", `is`(baseDay)),
                    hasProperty("eventDay", `is`(baseDay)),
                    hasProperty("valueDay", `is`(basePlus3Day)),
                    hasProperty("targetFiCode", `is`(Remarks.CashOut + "-" + ccy)),
                    hasProperty("targetFiAccountId", `is`("FI$accId")),
                    hasProperty("selfFiCode", `is`(Remarks.CashOut + "-" + ccy)),
                    hasProperty("selfFiAccountId", `is`("xxxxxx")),
                    hasProperty("statusType", `is`(ActionStatusType.Unprocessed)),
                    hasProperty("cashflowId", `is`(nullValue()))))

            // 拘束額を考慮した出金依頼 [例外]
            try {
                CashInOut.withdraw(rep(), businessDay, RegCashOut(accId, ccy, BigDecimal("701")))
                fail()
            } catch (e: ValidationException) {
                assertThat(e.message, `is`(AssetErrorKeys.CashInOutWithdrawAmount))
            }
        }
    }

    @Test
    fun 振込出金依頼を取消する() {
        val baseDay = businessDay.day()
        tx {
            // CF未発生の依頼を取消
            val normal = fixtures().cio(accId, "300", true).save(rep())
            assertThat(normal.cancel(rep()), hasProperty("statusType", `is`(ActionStatusType.Cancelled)))

            // 発生日を迎えた場合は取消できない [例外]
            val today = fixtures()
                    .cio(accId, "300", true).copy(eventDay = baseDay)
                    .save(rep())
            try {
                today.cancel(rep())
                fail()
            } catch (e: ValidationException) {
                assertThat(e.message, `is`(AssetErrorKeys.CashInOutBeforeEqualsDay))
            }
        }
    }

    @Test
    fun 振込出金依頼を例外状態とする() {
        val baseDay = businessDay.day()
        tx {
            val normal = fixtures().cio(accId, "300", true).save(rep())
            assertThat(normal.error(rep()), hasProperty("statusType", `is`(ActionStatusType.Error)))

            // 処理済の時はエラーにできない [例外]
            val today = fixtures().cio(accId, "300", true)
                    .copy(eventDay = baseDay, statusType = ActionStatusType.Processed)
                    .save(rep())
            try {
                today.error(rep())
                fail()
            } catch (e: ValidationException) {
                assertThat(e.message, `is`(ErrorKeys.ActionUnprocessing))
            }
        }
    }

    @Test
    fun 発生日を迎えた振込入出金をキャッシュフロー登録する() {
        val baseDay = businessDay.day()
        val basePlus3Day = businessDay.day(3)
        tx {
            // 発生日未到来の処理 [例外]
            val future = fixtures().cio(accId, "300", true).save(rep())
            try {
                future.process(rep())
                fail()
            } catch (e: ValidationException) {
                assertThat(e.message, `is`(AssetErrorKeys.CashInOutAfterEqualsDay))
            }

            // 発生日到来処理
            val normal = fixtures().cio(accId, "300", true)
                    .copy(eventDay = baseDay)
                    .save(rep())
            assertThat(normal.process(rep()), allOf(
                    hasProperty("statusType", `is`(ActionStatusType.Processed)),
                    hasProperty("cashflowId", not(nullValue()))))
            // 発生させたキャッシュフローの検証
            assertThat(Cashflow.load(rep(), normal.cashflowId), allOf(
                    hasProperty("accountId", `is`(accId)),
                    hasProperty("currency", `is`(ccy)),
                    hasProperty("amount", `is`(BigDecimal("-300"))),
                    hasProperty("cashflowType", `is`(CashflowType.CashOut)),
                    hasProperty("remark", `is`(Remarks.CashOut)),
                    hasProperty("eventDay", `is`(baseDay)),
                    hasProperty("valueDay", `is`(basePlus3Day)),
                    hasProperty("statusType", `is`(ActionStatusType.Unprocessed))))
        }
    }

    companion object {
        private const val ccy = "JPY"
        private const val accId = "test"
    }

}
