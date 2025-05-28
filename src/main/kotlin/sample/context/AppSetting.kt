package sample.context

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.util.*
import sample.context.orm.MatchMode
import sample.context.orm.OrmActiveRecord
import sample.context.orm.OrmRepository
import sample.model.constraints.OutlineEmpty

/**
 * Represents application setting information. <p>Assumes initial data is registered in advance and
 * only allows value changes.
 */
@Entity
data class AppSetting(
        /** Setting ID */
        @Id @field:Size(max = 120) val id: String,
        /** Category */
        @field:Size(max = 60) var category: String,
        /** Outline */
        @field:Size(max = 1300) var outline: String,
        /** Value */
        @field:Size(max = 1300) var value: String?
) : OrmActiveRecord<AppSetting>() {

    /** Gets the setting information value. */
    fun str(): String = value!!

    fun str(defaultValue: String): String {
        return if (value == null) defaultValue else value!!
    }

    fun intValue(): Int = value!!.toInt()
    fun intValue(defaultValue: Int): Int =
            if (value == null) defaultValue else Integer.parseInt(value)

    fun longValue(): Long = value!!.toLong()
    fun longValue(defaultValue: Long): Long = if (value == null) defaultValue else value!!.toLong()

    fun bool(): Boolean = value!!.toBoolean()
    fun bool(defaultValue: Boolean): Boolean =
            if (value == null) defaultValue else value!!.toBoolean()

    fun decimal(): BigDecimal = BigDecimal(value)
    fun decimal(defaultValue: BigDecimal): BigDecimal =
            if (value == null) defaultValue else BigDecimal(value)

    /** Sets the setting information value. */
    fun update(rep: OrmRepository, value: String): AppSetting {
        this.value = value
        return update(rep)
    }

    companion object {
        private const val serialVersionUID: Long = 1

        /** Gets setting information. */
        fun get(rep: OrmRepository, id: String): Optional<AppSetting> {
            return rep.get(AppSetting::class.java, id)
        }

        fun load(rep: OrmRepository, id: String): AppSetting {
            return rep.load(AppSetting::class.java, id)
        }

        /** Searches application setting information. */
        fun find(rep: OrmRepository, p: FindAppSetting): List<AppSetting> {
            return rep.tmpl().findByCriteria(AppSetting::class.java) { criteria ->
                criteria.like(arrayOf("id", "category", "outline"), p.keyword, MatchMode.ANYWHERE)
                        .result()
            }
        }
    }
}

data class FindAppSetting(@field:OutlineEmpty val keyword: String? = null)
