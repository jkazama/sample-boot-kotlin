package sample.model.account

import org.hamcrest.Matchers.*
import org.junit.Assert.*
import org.junit.Test
import sample.EntityTestSupport
import sample.ErrorKeys
import sample.ValidationException

class FiAccountTest : EntityTestSupport() {

    override fun setupPreset() {
        targetEntities(FiAccount::class.java, Account::class.java)
    }

    override fun before() {
        tx {
            fixtures().fiAcc("normal", "sample", "JPY").save(rep())
        }
    }

    @Test
    fun 金融機関口座を取得する() {
        tx {
            assertThat(FiAccount.load(rep(), "normal", "sample", "JPY"), allOf(
                    hasProperty("accountId", `is`("normal")),
                    hasProperty("category", `is`("sample")),
                    hasProperty("currency", `is`("JPY")),
                    hasProperty("fiCode", `is`("sample-JPY")),
                    hasProperty("fiAccountId", `is`("FInormal"))))
            try {
                FiAccount.load(rep(), "normal", "sample", "USD")
                fail()
            } catch (e: ValidationException) {
                assertThat(e.message, `is`(ErrorKeys.EntityNotFound))
            }
        }
    }

    @Test
    fun Hibernate5_1で追加されたアドホックなJoin検証() {
        tx {
            fixtures().fiAcc("sample", "join", "JPY").save(rep())
            fixtures().acc("sample").save(rep())

            val list = rep().tmpl()
                    .find<Array<Any>>("FROM FiAccount fa LEFT JOIN Account a ON fa.accountId = a.id WHERE fa.accountId = ?1", "sample")
                    .map { mapJoin(it) }

            assertFalse(list.isEmpty())
            val (accountId, name, fiCode) = list[0]
            assertThat(accountId, `is`("sample"))
            assertThat(name, `is`("sample"))
            assertThat(fiCode, `is`("join-JPY"))
        }
    }

    private fun mapJoin(values: Array<Any>): FiAccountJoin {
        val fa = values[0] as FiAccount
        val a = values[1] as Account
        return FiAccountJoin(fa.accountId, a.name, fa.fiCode, fa.fiAccountId)
    }

    data class FiAccountJoin(
            val accountId: String,
            val name: String,
            val fiCode: String,
            val fiAccountId: String)
}
