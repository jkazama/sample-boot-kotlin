package sample.model.account

import org.hamcrest.Matchers.*
import org.junit.Assert.*
import org.junit.Test
import sample.EntityTestSupport
import sample.ErrorKeys
import sample.ValidationException


class LoginTest : EntityTestSupport() {
    override fun setupPreset() {
        targetEntities(Login::class.java)
    }

    override fun before() {
        tx { fixtures().login("test").save(rep()) }
    }

    @Test
    fun ログインIDを変更する() {
        tx {
            // 正常系
            fixtures().login("any").save(rep())
            assertThat(Login.load(rep(), "any").change(rep(), ChgLoginId("testAny")), allOf(
                    hasProperty("id", `is`("any")),
                    hasProperty("loginId", `is`("testAny"))))

            // 自身に対する同名変更
            assertThat(Login.load(rep(), "any").change(rep(), ChgLoginId("testAny")), allOf(
                    hasProperty("id", `is`("any")),
                    hasProperty("loginId", `is`("testAny"))))

            // 重複ID
            try {
                Login.load(rep(), "any").change(rep(), ChgLoginId("test"))
                fail()
            } catch (e: ValidationException) {
                assertThat(e.message, `is`(ErrorKeys.DuplicateId))
            }
        }
    }

    @Test
    fun パスワードを変更する() {
        tx {
            val login = Login.load(rep(), "test").change(rep(), encoder, ChgPassword("changed"))
            assertTrue(encoder.matches("changed", login.password))
        }
    }

    @Test
    fun ログイン情報を取得する() {
        tx {
            val m = Login.load(rep(), "test")
            m.loginId = "changed"
            m.update(rep())
            assertTrue(Login.getByLoginId(rep(), "changed").isPresent)
            assertFalse(Login.getByLoginId(rep(), "test").isPresent)
        }
    }

}
