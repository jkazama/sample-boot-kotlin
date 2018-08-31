package sample.model.master

import org.springframework.format.annotation.DateTimeFormat
import sample.context.Dto
import sample.context.orm.OrmActiveMetaRecord
import sample.context.orm.OrmRepository
import sample.model.constraints.*
import sample.util.DateUtils
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.validation.Valid
import javax.validation.constraints.NotNull


/**
 * 休日マスタを表現します。
 */
@Entity
data class Holiday(
        /** ID  */
        @Id
        @GeneratedValue
        var id: Long? = null,
        /** 休日区分  */
        @field:Category
        val category: String,
        /** 休日  */
        @field:NotNull
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        val day: LocalDate,
        /** 休日名称  */
        @field:Name(max = 40)
        val name: String,
        override var createId: String? = null,
        override var createDate: LocalDateTime? = null,
        override var updateId: String? = null,
        override var updateDate: LocalDateTime? = null
) : OrmActiveMetaRecord<Holiday>() {

    companion object {
        private const val serialVersionUID = 1L
        val CategoryDefault = "default"

        /** 休日マスタを取得します。  */
        fun get(rep: OrmRepository, day: LocalDate, category: String = CategoryDefault): Optional<Holiday> =
                rep.tmpl().get("FROM Holiday h WHERE h.category=?1 AND h.day=?2", category, day)

        /** 休日マスタを取得します。(例外付)  */
        @JvmOverloads
        fun load(rep: OrmRepository, day: LocalDate, category: String = CategoryDefault): Holiday =
                rep.tmpl().load("FROM Holiday h WHERE h.category=?1 AND h.day=?2", category, day)

        /** 休日情報を検索します。  */
        fun find(rep: OrmRepository, year: Int, category: String = CategoryDefault): List<Holiday> =
                rep.tmpl().find("FROM Holiday h WHERE h.category=?1 AND h.day BETWEEN ?2 AND ?3 ORDER BY h.day",
                        category, LocalDate.ofYearDay(year, 1), DateUtils.dayTo(year))

        /** 休日マスタを登録します。  */
        fun register(rep: OrmRepository, p: RegHoliday) {
            rep.tmpl().execute("DELETE FROM Holiday h WHERE h.category=?1 AND h.day BETWEEN ?2 AND ?3",
                    p.category, LocalDate.ofYearDay(p.year!!, 1), DateUtils.dayTo(p.year))
            p.list.forEach { v -> v.create(p).save(rep) }
        }
    }

}

/** 登録パラメタ  */
data class RegHoliday(
        @field:Year
        val year: Int? = null,
        @field:CategoryEmpty
        val category: String = Holiday.CategoryDefault,
        @Valid
        val list: List<RegHolidayItem> = listOf()) : Dto

/** 登録パラメタ(要素)  */
data class RegHolidayItem(
        @field:NotNull
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        val day: LocalDate? = null,
        @field:Name(max = 40)
        val name: String? = null
) : Dto {
    fun create(p: RegHoliday): Holiday =
            Holiday(
                    category = p.category,
                    day = this.day!!,
                    name = this.name!!
            )
}
