package sample.util

import org.apache.commons.lang3.StringUtils
import org.springframework.util.Assert
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import java.time.temporal.TemporalAccessor
import java.time.temporal.TemporalQuery
import java.util.*


/**
 * 頻繁に利用される日時ユーティリティを表現します。
 */
object DateUtils {
    private val weekendQuery = WeekendQuery()

    /** 指定された文字列(YYYY-MM-DD)を元に日付へ変換します。  */
    fun day(dayStr: String): LocalDate =
            dayOpt(dayStr).orElse(null)

    fun dayOpt(dayStr: String): Optional<LocalDate> =
            if (StringUtils.isBlank(dayStr)) Optional.empty()
            else Optional.of(LocalDate.parse(dayStr.trim(), DateTimeFormatter.ISO_LOCAL_DATE))

    /** 指定された文字列とフォーマット型を元に日時へ変換します。  */
    fun date(dateStr: String, formatter: DateTimeFormatter): LocalDateTime =
            dateOpt(dateStr, formatter).orElse(null)

    fun dateOpt(dateStr: String, formatter: DateTimeFormatter): Optional<LocalDateTime> =
            if (StringUtils.isBlank(dateStr)) Optional.empty()
            else Optional.of(LocalDateTime.parse(dateStr.trim(), formatter))

    /** 指定された文字列とフォーマット文字列を元に日時へ変換します。  */
    fun date(dateStr: String, format: String): LocalDateTime =
            date(dateStr, DateTimeFormatter.ofPattern(format))

    fun dateOpt(dateStr: String, format: String): Optional<LocalDateTime> =
            dateOpt(dateStr, DateTimeFormatter.ofPattern(format))

    /** 指定された日付を日時へ変換します。 */
    fun dateByDay(day: LocalDate): LocalDateTime =
            dateByDayOpt(day).orElse(null)

    fun dateByDayOpt(day: LocalDate): Optional<LocalDateTime> =
            Optional.ofNullable(day).map { it.atStartOfDay() }

    /** 指定した日付の翌日から1msec引いた日時を返します。  */
    fun dateTo(day: LocalDate): LocalDateTime =
            dateToOpt(day).orElse(null)

    fun dateToOpt(day: LocalDate): Optional<LocalDateTime> =
            Optional.ofNullable(day).map { it.atTime(23, 59, 59) }

    /** 指定された日時型とフォーマット型を元に文字列(YYYY-MM-DD)へ変更します。  */
    fun dayFormat(day: LocalDate): String =
            dayFormatOpt(day).orElse(null)

    fun dayFormatOpt(day: LocalDate): Optional<String> =
            Optional.ofNullable(day).map { it.format(DateTimeFormatter.ISO_LOCAL_DATE) }

    /** 指定された日時型とフォーマット型を元に文字列へ変更します。  */
    fun dateFormat(date: LocalDateTime, formatter: DateTimeFormatter): String =
            dateFormatOpt(date, formatter).orElse(null)

    fun dateFormatOpt(date: LocalDateTime, formatter: DateTimeFormatter): Optional<String> =
            Optional.ofNullable(date).map { it.format(formatter) }

    /** 指定された日時型とフォーマット文字列を元に文字列へ変更します。  */
    fun dateFormat(date: LocalDateTime, format: String): String =
            dateFormatOpt(date, format).orElse(null)

    fun dateFormatOpt(date: LocalDateTime, format: String): Optional<String> =
            Optional.ofNullable(date).map { it.format(DateTimeFormatter.ofPattern(format)) }

    /** 日付の間隔を取得します。  */
    fun between(start: LocalDate?, end: LocalDate?): Optional<Period> =
            if (start == null || end == null) Optional.empty()
            else Optional.of(Period.between(start, end))

    /** 日時の間隔を取得します。  */
    fun between(start: LocalDateTime?, end: LocalDateTime?): Optional<Duration> =
            if (start == null || end == null) Optional.empty()
            else Optional.of(Duration.between(start, end))

    /** 指定営業日が週末(土日)か判定します。(引数は必須)  */
    fun isWeekend(day: LocalDate): Boolean {
        Assert.notNull(day, "day is required.")
        return day.query(weekendQuery)
    }

    /** 指定年の最終日を取得します。  */
    fun dayTo(year: Int): LocalDate =
            LocalDate.ofYearDay(year, if (Year.of(year).isLeap) 366 else 365)

}

/** 週末判定用のTemporalQuery&gt;Boolean&lt;を表現します。  */
class WeekendQuery : TemporalQuery<Boolean> {
    override fun queryFrom(temporal: TemporalAccessor): Boolean {
        val dayOfWeek = DayOfWeek.of(temporal.get(ChronoField.DAY_OF_WEEK))
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY
    }
}
