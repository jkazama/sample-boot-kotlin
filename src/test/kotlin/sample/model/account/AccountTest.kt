package sample.model.account

import org.hamcrest.Matchers.*
import org.junit.Assert.*
import org.junit.Test
import sample.ErrorKeys
import sample.EntityTestSupport
import sample.ValidationException

class AccountTest : EntityTestSupport() {

    override fun setupPreset() {
        targetEntities(Account::class.java, Login::class.java)
    }

    override fun before() {
        tx { fixtures().acc("normal").save(rep()) }
    }

    @Test
    fun 口座情報を登録する() {
        tx {
            // 通常登録
            assertFalse(Account.get(rep(), "new").isPresent)
            Account.register(rep(), encoder, RegAccount("new", "name", "new@example.com", "password"))
            assertThat(Account.load(rep(), "new"), allOf(
                    hasProperty("name", `is`("name")),
                    hasProperty("mail", `is`("new@example.com"))))
            val login = Login.load(rep(), "new")
            assertTrue(encoder.matches("password", login.password))
            // 同一ID重複
            try {
                Account.register(rep(), encoder, RegAccount("normal", "name", "new@example.com", "password"))
                fail()
            } catch (e: ValidationException) {
                assertThat(e.message, `is`(ErrorKeys.DuplicateId))
            }
        }
    }

    @Test
    fun 口座情報を変更する() {
        tx {
            Account.load(rep(), "normal").change(rep(), ChgAccount("changed", "changed@example.com"))
            assertThat(Account.load(rep(), "normal"), allOf(
                    hasProperty("name", `is`("changed")),
                    hasProperty("mail", `is`("changed@example.com"))))
        }
    }

    @Test
    fun 有効口座を取得する() {
        tx {
            // 通常時取得
            assertThat(Account.loadValid(rep(), "normal"), allOf(
                    hasProperty("id", `is`("normal")),
                    hasProperty("statusType", `is`(AccountStatusType.Normal))))

            // 退会時取得
            val withdrawal = fixtures().acc("withdrawal")
            withdrawal.statusType = AccountStatusType.Withdrawal
            withdrawal.save(rep())
            try {
                Account.loadValid(rep(), "withdrawal")
                fail()
            } catch (e: ValidationException) {
                assertThat(e.message, `is`("error.Account.loadValid"))
            }
        }
    }
}
