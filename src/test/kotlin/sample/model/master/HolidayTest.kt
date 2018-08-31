package sample.model.master

import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.hasSize
import org.junit.Assert.assertThat
import org.junit.Assert.assertTrue
import org.junit.Test
import sample.EntityTestSupport
import sample.util.DateUtils
import java.time.LocalDate


class HolidayTest : EntityTestSupport() {

    override fun setupPreset() {
        targetEntities(Holiday::class.java)
    }

    override fun before() {
        tx {
            listOf("2015-09-21", "2015-09-22", "2015-09-23", "2016-09-21")
                    .map { fixtures().holiday(it) }
                    .onEach { it.save(rep()) }
        }
    }

    @Test
    fun 休日を取得する() {
        tx {
            val day = Holiday.get(rep(), LocalDate.of(2015, 9, 22))
            assertTrue(day.isPresent)
            assertThat(day.get().day, `is`(LocalDate.of(2015, 9, 22)))
        }
    }

    @Test
    fun 休日を検索する() {
        tx {
            assertThat(Holiday.find(rep(), 2015), hasSize(3))
            assertThat(Holiday.find(rep(), 2016), hasSize(1))
        }
    }

    @Test
    fun 休日を登録する() {
        val items = listOf("2016-09-21", "2016-09-22", "2016-09-23")
                .map { RegHolidayItem(DateUtils.day(it), "休日") }
        tx {
            Holiday.register(rep(), RegHoliday(year = 2016, list = items))
            assertThat(Holiday.find(rep(), 2016), hasSize(3))
        }
    }
}
