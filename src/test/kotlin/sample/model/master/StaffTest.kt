package sample.model.master

import org.junit.Assert.*
import org.junit.Assert.assertThat
import org.hamcrest.MatcherAssert.*
import org.hamcrest.Matchers.*
import org.junit.Test
import sample.ErrorKeys
import sample.EntityTestSupport
import sample.ValidationException

class StaffTest : EntityTestSupport() {

    override fun setupPreset() {
        targetEntities(Staff::class.java)
    }

    override fun before() {
        tx { fixtures().staff("sample").save(rep()) }
    }

    @Test
    fun 社員情報を登録する() {
        tx {
            // 正常登録
            val staff = Staff.register(rep(), encoder, RegStaff("new", "newName", "password"))
            assertThat(staff, allOf(
                    hasProperty("id", `is`("new")),
                    hasProperty("name", `is`("newName"))))
            assertTrue(encoder.matches("password", staff.password))

            // 重複ID
            try {
                Staff.register(rep(), encoder, RegStaff("sample", "newName", "password"))
                fail()
            } catch (e: ValidationException) {
                assertThat(e.message, `is`(ErrorKeys.DuplicateId))
            }
        }
    }

    @Test
    fun 社員パスワードを変更する() {
        tx {
            val changed = Staff.load(rep(), "sample").change(rep(), encoder, ChgPassword("changed"))
            assertTrue(encoder.matches("changed", changed.password))
        }
    }

    @Test
    fun 社員情報を変更する() {
        tx {
            assertThat(
                    Staff.load(rep(), "sample").change(rep(), ChgStaff("changed")).name, `is`("changed"))
        }
    }

    @Test
    fun 社員を検索する() {
        tx {
            assertFalse(Staff.find(rep(), FindStaff("amp")).isEmpty())
            assertTrue(Staff.find(rep(), FindStaff("amq")).isEmpty())
        }
    }

}

