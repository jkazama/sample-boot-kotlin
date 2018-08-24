package sample.model.master

import org.hamcrest.Matchers.*
import org.junit.Assert.assertThat
import org.junit.Assert.fail
import org.junit.Test
import sample.EntityTestSupport
import sample.ErrorKeys
import sample.ValidationException

class SelfFiAccountTest : EntityTestSupport() {

    override fun setupPreset() {
        targetEntities(SelfFiAccount::class.java)
    }

    override fun before() {
        tx { fixtures().selfFiAcc("sample", "JPY").save(rep()) }
    }

    @Test
    fun 自社金融機関口座を取得する() {
        tx {
            assertThat(SelfFiAccount.load(rep(), "sample", "JPY"), allOf(
                    hasProperty("category", `is`("sample")),
                    hasProperty("currency", `is`("JPY")),
                    hasProperty("fiCode", `is`("sample-JPY")),
                    hasProperty("fiAccountId", `is`("xxxxxx"))))
            try {
                SelfFiAccount.load(rep(), "sample", "USD")
                fail()
            } catch (e: ValidationException) {
                assertThat(e.message, `is`(ErrorKeys.EntityNotFound))
            }
        }
    }

}
