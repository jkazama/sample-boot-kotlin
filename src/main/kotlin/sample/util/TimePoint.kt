package sample.util

import org.springframework.format.annotation.DateTimeFormat
import java.io.Serializable
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import javax.validation.constraints.NotNull

/**
 * 日付と日時のペアを表現します。
 * <p>0:00に営業日切り替えが行われないケースなどでの利用を想定しています。
 */
data class TimePoint(
        @field:NotNull
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        var day: LocalDate,
        @field:NotNull
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        var date: LocalDateTime = day.atStartOfDay()) : Serializable {

    /** 指定日付と同じか。(day == targetDay)  */
    fun equalsDay(targetDay: LocalDate): Boolean =
            day.compareTo(targetDay) == 0

    /** 指定日付よりも前か。(day &lt; targetDay)  */
    fun beforeDay(targetDay: LocalDate): Boolean =
            day.compareTo(targetDay) < 0

    /** 指定日付以前か。(day &lt;= targetDay)  */
    fun beforeEqualsDay(targetDay: LocalDate): Boolean =
            day.compareTo(targetDay) <= 0

    /** 指定日付よりも後か。(targetDay &lt; day)  */
    fun afterDay(targetDay: LocalDate): Boolean =
            0 < day.compareTo(targetDay)

    /** 指定日付以降か。(targetDay &lt;= day)  */
    fun afterEqualsDay(targetDay: LocalDate): Boolean =
            0 <= day.compareTo(targetDay)

    companion object {
        private const val serialVersionUID: Long = 1;

        /** TimePointを生成します。  */
        fun now(): TimePoint {
            val now = LocalDateTime.now()
            return TimePoint(now.toLocalDate(), now)
        }

        /** TimePointを生成します。  */
        fun now(clock: Clock): TimePoint {
            val now = LocalDateTime.now(clock)
            return TimePoint(now.toLocalDate(), now)
        }
    }
}