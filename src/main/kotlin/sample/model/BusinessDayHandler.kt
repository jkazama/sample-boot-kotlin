package sample.model

import org.springframework.beans.factory.ObjectProvider
import sample.context.orm.DefaultRepository
import org.springframework.cache.annotation.CacheEvict
import java.time.LocalDate
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import sample.context.SimpleObjectProvider
import sample.context.Timestamper
import sample.model.master.Holiday
import sample.model.master.RegHoliday
import sample.util.DateUtils
import java.util.*


/**
 * ドメインに依存する営業日関連のユーティリティハンドラ。
 */
open class BusinessDayHandler(
        val time: Timestamper,
        val holidayAccessor: ObjectProvider<HolidayAccessor> = SimpleObjectProvider(null)
) {

    /** 営業日を返します。  */
    fun day(): LocalDate {
        return time.day()
    }

    /** 営業日を返します。  */
    fun day(daysToAdd: Int): LocalDate {
        var day = day()
        if (0 < daysToAdd) {
            for (i in 0 until daysToAdd)
                day = dayNext(day)
        } else if (daysToAdd < 0) {
            for (i in 0 until -daysToAdd)
                day = dayPrevious(day)
        }
        return day
    }

    private fun dayNext(baseDay: LocalDate): LocalDate {
        var day = baseDay.plusDays(1)
        while (isHolidayOrWeekDay(day))
            day = day.plusDays(1)
        return day
    }

    private fun dayPrevious(baseDay: LocalDate): LocalDate {
        var day = baseDay.minusDays(1)
        while (isHolidayOrWeekDay(day))
            day = day.minusDays(1)
        return day
    }

    /** 祝日もしくは週末時はtrue。  */
    private fun isHolidayOrWeekDay(day: LocalDate): Boolean =
        DateUtils.isWeekend(day) || isHoliday(day)

    private fun isHoliday(day: LocalDate): Boolean =
        if (holidayAccessor.ifAvailable == null) {
            false
        } else {
            holidayAccessor.getObject().getHoliday(day).isPresent
        }

    /** 祝日マスタを検索/登録するアクセサ。  */
    @Component
    class HolidayAccessor(val rep: DefaultRepository) {

        @Transactional(DefaultRepository.BeanNameTx)
        @Cacheable(cacheNames = ["HolidayAccessor.getHoliday"])
        fun getHoliday(day: LocalDate): Optional<Holiday> {
            return Holiday.get(rep, day)
        }

        @Transactional(DefaultRepository.BeanNameTx)
        @CacheEvict(cacheNames = ["HolidayAccessor.getHoliday"], allEntries = true)
        fun register(rep: DefaultRepository, p: RegHoliday) {
            Holiday.register(rep, p)
        }

    }

}
