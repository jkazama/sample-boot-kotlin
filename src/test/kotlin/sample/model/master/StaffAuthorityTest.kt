package sample.model.master

import jdk.nashorn.internal.objects.NativeArray.forEach
import org.hamcrest.Matchers.`is`
import org.junit.Assert.assertThat
import org.junit.Test
import sample.EntityTestSupport


class StaffAuthorityTest : EntityTestSupport() {

    override fun setupPreset() {
        targetEntities(StaffAuthority::class.java)
    }

    override fun before() {
        tx {
            fixtures().staffAuth("staffA", "ID000001", "ID000002", "ID000003").forEach { auth -> auth.save(rep()) }
            fixtures().staffAuth("staffB", "ID000001", "ID000002").forEach { auth -> auth.save(rep()) }
        }
    }

    @Test
    fun 権限一覧を検索する() {
        tx {
            assertThat(StaffAuthority.find(rep(), "staffA").size, `is`(3))
            assertThat(StaffAuthority.find(rep(), "staffB").size, `is`(2))
        }
    }

}
