package sample.context

import org.hibernate.criterion.MatchMode
import sample.context.orm.OrmActiveRecord
import sample.context.orm.OrmRepository
import sample.model.constraints.OutlineEmpty
import java.math.BigDecimal
import java.util.*
import javax.persistence.Entity
import javax.persistence.Id
import javax.validation.constraints.Size


/**
 * アプリケーション設定情報を表現します。
 * <p>事前に初期データが登録される事を前提とし、値の変更のみ許容します。
 */
@Entity
data class AppSetting(
        /** 設定ID */
        @Id
        @Size(max = 120)
        val id: String,
        /** 区分 */
        @Size(max = 60)
        var category: String,
        /** 概要 */
        @Size(max = 1300)
        var outline: String,
        /** 値 */
        @Size(max = 1300)
        var value: String?
) : OrmActiveRecord<AppSetting>() {

    /** 設定情報値を取得します。  */
    fun str(): String = value!!

    fun str(defaultValue: String): String {
        return if (value == null) defaultValue else value!!
    }

    fun intValue(): Int = value!!.toInt()
    fun intValue(defaultValue: Int): Int {
        return if (value == null) defaultValue else Integer.parseInt(value)
    }

    fun longValue(): Long = value!!.toLong()
    fun longValue(defaultValue: Long): Long {
        return if (value == null) defaultValue else value!!.toLong()
    }

    fun bool(): Boolean = value!!.toBoolean()
    fun bool(defaultValue: Boolean): Boolean {
        return if (value == null) defaultValue else value!!.toBoolean()
    }

    fun decimal(): BigDecimal = BigDecimal(value)
    fun decimal(defaultValue: BigDecimal): BigDecimal {
        return if (value == null) defaultValue else BigDecimal(value)
    }

    /** 設定情報値を設定します。  */
    fun update(rep: OrmRepository, value: String): AppSetting {
        this.value = value
        return update(rep)
    }

    companion object {
        private const val serialVersionUID: Long = 1

        /** 設定情報を取得します。  */
        fun get(rep: OrmRepository, id: String): Optional<AppSetting> {
            return rep.get(AppSetting::class.java, id)
        }

        fun load(rep: OrmRepository, id: String): AppSetting {
            return rep.load(AppSetting::class.java, id)
        }

        /** アプリケーション設定情報を検索します。  */
        fun find(rep: OrmRepository, p: FindAppSetting): List<AppSetting> {
            return rep.tmpl().findByCriteria(AppSetting::class.java, { criteria ->
                criteria
                        .like(arrayOf("id", "category", "outline"), p.keyword, MatchMode.ANYWHERE)
                        .result()
            })
        }
    }
}

data class FindAppSetting(
        @OutlineEmpty
        val keyword: String
)
