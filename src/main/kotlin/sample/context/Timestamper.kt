package sample.context

import org.springframework.stereotype.Component
import sample.util.DateUtils
import sample.util.TimePoint
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 日時ユーティリティコンポーネント。
 */
open class Timestamper(
        val clock: Clock = Clock.systemDefaultZone(),
        val setting: AppSettingHandler? = null
) {
    /** 営業日を返します。  */
    fun day(): LocalDate =
            if (setting == null) LocalDate.now(clock) else DateUtils.day(setting.setting(KeyDay).str())
    /** 日時を返します。  */
    fun date(): LocalDateTime =
        LocalDateTime.now(clock)
    /** 営業日/日時を返します。  */
    fun tp(): TimePoint =
        TimePoint(day(), date())

    /**
     * 営業日を指定日へ進めます。
     * AppSettingHandlerを設定時のみ有効です。
     * @param day 更新営業日
     */
    fun proceedDay(day: LocalDate): Timestamper {
        setting?.update(KeyDay, DateUtils.dayFormat(day))
        return this
    }

    companion object {
        const val KeyDay = "system.businessDay.day"
    }
}