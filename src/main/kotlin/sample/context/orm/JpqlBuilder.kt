package sample.context.orm

import org.apache.commons.lang3.StringUtils
import org.hibernate.criterion.MatchMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger


/**
 * 簡易にJPQLを生成するためのビルダー。
 * <p>条件句の動的条件生成に特化させています。
 */
class JpqlBuilder(val jpql: StringBuilder, val index: AtomicInteger) {
    private val conditions: MutableList<String> = mutableListOf()
    private val reservedArgs: MutableList<Any> = mutableListOf()
    private val args: MutableList<Any> = mutableListOf()
    private var orderBy: Optional<String> = Optional.empty()

    private fun add(condition: String?): JpqlBuilder {
        if (!condition.isNullOrBlank()) {
            this.conditions.add(condition.orEmpty())
        }
        return this
    }

    private fun reservedArgs(vararg args: Any): JpqlBuilder {
        if (args.isNotEmpty()) {
            args.forEach { this.reservedArgs.add(it) }
        }
        return this
    }

    /** 一致条件を付与します。(値がnullの時は無視されます)  */
    fun equal(field: String, value: Any?): JpqlBuilder =
            ifValid(value, {
                conditions.add(String.format("%s = ?%d", field, index.getAndIncrement()))
                args.add(value!!)
            })

    private fun ifValid(value: Any?, command: () -> Unit): JpqlBuilder {
        if (isValid(value)) {
            command()
        }
        return this
    }

    private fun isValid(value: Any?): Boolean =
            if (value is String) {
                StringUtils.isNotBlank(value)
            } else if (value is Optional<*>) {
                value.isPresent
            } else if (value is Array<*>) {
                0 < value.size
            } else if (value is Collection<*>) {
                0 < value.size
            } else {
                value != null
            }

    /** 不一致条件を付与します。(値がnullの時は無視されます)  */
    fun equalNot(field: String, value: Any?): JpqlBuilder =
            ifValid(value) {
                conditions.add(String.format("%s != ?%d", field, index.getAndIncrement()))
                args.add(value!!)
            }

    /** like条件を付与します。(値がnullの時は無視されます)  */
    fun like(field: String, value: String?, mode: MatchMode): JpqlBuilder =
            ifValid(value) {
                conditions.add(String.format("%s like ?%d", field, index.getAndIncrement()))
                args.add(mode.toMatchString(value))
            }

    /** like条件を付与します。[複数フィールドに対するOR結合](値がnullの時は無視されます)  */
    fun like(fields: List<String>, value: String?, mode: MatchMode): JpqlBuilder =
            ifValid(value) {
                val condition = StringBuilder("(")
                for (field in fields) {
                    if (condition.length != 1) {
                        condition.append(" or ")
                    }
                    condition.append(String.format("(%s like ?%d)", field, index.getAndIncrement()))
                    args.add(mode.toMatchString(value))
                }
                condition.append(")")
                conditions.add(condition.toString())
            }

    /** in条件を付与します。  */
    fun `in`(field: String, values: List<Any>): JpqlBuilder =
            ifValid(values) {
                conditions.add(String.format("%s in ?%d", field, index.getAndIncrement()))
                args.add(values)
            }

    /** between条件を付与します。  */
    fun between(field: String, from: Date?, to: Date?): JpqlBuilder {
        if (from != null && to != null) {
            conditions.add(String.format(
                    "%s between ?%d and ?%d", field, index.getAndIncrement(), index.getAndIncrement()))
            args.add(from)
            args.add(to)
        } else if (from != null) {
            gte(field, from)
        } else if (to != null) {
            lte(field, to)
        }
        return this
    }

    /** between条件を付与します。  */
    fun between(field: String, from: LocalDate?, to: LocalDate?): JpqlBuilder {
        if (from != null && to != null) {
            conditions.add(String.format(
                    "%s between ?%d and ?%d", field, index.getAndIncrement(), index.getAndIncrement()))
            args.add(from)
            args.add(to)
        } else if (from != null) {
            gte(field, from)
        } else if (to != null) {
            lte(field, to)
        }
        return this
    }

    /** between条件を付与します。  */
    fun between(field: String, from: LocalDateTime?, to: LocalDateTime?): JpqlBuilder {
        if (from != null && to != null) {
            conditions.add(String.format(
                    "%s between ?%d and ?%d", field, index.getAndIncrement(), index.getAndIncrement()))
            args.add(from)
            args.add(to)
        } else if (from != null) {
            gte(field, from)
        } else if (to != null) {
            lte(field, to)
        }
        return this
    }

    /** between条件を付与します。  */
    fun between(field: String, from: String?, to: String?): JpqlBuilder {
        if (isValid(from) && isValid(to)) {
            conditions.add(String.format(
                    "%s between ?%d and ?%d", field, index.getAndIncrement(), index.getAndIncrement()))
            args.add(from!!)
            args.add(to!!)
        } else if (isValid(from)) {
            gte(field, from!!)
        } else if (isValid(to)) {
            lte(field, to!!)
        }
        return this
    }

    /** [フィールド]&gt;=[値] 条件を付与します。(値がnullの時は無視されます)  */
    fun <Y : Comparable<Y>> gte(field: String, value: Y?): JpqlBuilder =
            ifValid(value) {
                conditions.add(String.format("%s >= ?%d", field, index.getAndIncrement()))
                args.add(value!!)
            }

    /** [フィールド]&gt;[値] 条件を付与します。(値がnullの時は無視されます)  */
    fun <Y : Comparable<Y>> gt(field: String, value: Y?): JpqlBuilder =
            ifValid(value) {
                conditions.add(String.format("%s > ?%d", field, index.getAndIncrement()))
                args.add(value!!)
            }

    /** [フィールド]&lt;=[値] 条件を付与します。  */
    fun <Y : Comparable<Y>> lte(field: String, value: Y?): JpqlBuilder =
            ifValid(value) {
                conditions.add(String.format("%s <= ?%d", field, index.getAndIncrement()))
                args.add(value!!)
            }

    /** [フィールド]&lt;[値] 条件を付与します。  */
    fun <Y : Comparable<Y>> lt(field: String, value: Y?): JpqlBuilder =
            ifValid(value) {
                conditions.add(String.format("%s < ?%d", field, index.getAndIncrement()))
                args.add(value!!)
            }

    /** order by 条件句を付与します。  */
    fun orderBy(orderBy: String): JpqlBuilder {
        this.orderBy = Optional.ofNullable(orderBy)
        return this
    }

    /** JPQLを生成します。  */
    fun build(): String {
        val jpql = StringBuilder(this.jpql.toString())
        if (!conditions.isEmpty()) {
            jpql.append(" where ")
            val first = AtomicBoolean(true)
            conditions.forEach({ condition ->
                if (!first.getAndSet(false)) {
                    jpql.append(" and ")
                }
                jpql.append(condition)
            })
        }
        orderBy.ifPresent { v -> jpql.append(" order by $v") }
        return jpql.toString()
    }

    /** JPQLに紐付く実行引数を返します。  */
    fun args(): Array<Any> =
        (reservedArgs + args).toTypedArray()

    companion object {
        /**
         * ビルダーを生成します。
         * @param baseJpql 基点となるJPQL (where / order by は含めない)
         * @return ビルダー情報
         */
        fun of(baseJpql: String): JpqlBuilder =
                of(baseJpql, 1)

        /**
         * ビルダーを生成します。
         * @param baseJpql 基点となるJPQL (where / order by は含めない)
         * @param fromIndex 動的に付与する条件句の開始インデックス(1開始)。
         * 既に「field=?1」等で置換連番を付与しているときはその次番号。
         * @param args 既に付与済みの置換連番に紐づく引数
         * @return ビルダー情報
         */
        fun of(baseJpql: String, fromIndex: Int, vararg args: Any): JpqlBuilder =
                JpqlBuilder(StringBuilder(baseJpql), AtomicInteger(fromIndex)).reservedArgs(*args)

        /**
         * ビルダーを生成します。
         * @param baseJpql 基点となるJPQL (where / order by は含めない)
         * @param staticCondition 条件指定無しに確定する where 条件句 (field is null 等)
         * @return ビルダー情報
         */
        fun of(baseJpql: String, staticCondition: String): JpqlBuilder =
                of(baseJpql, staticCondition, 1)

        /**
         * ビルダーを生成します。
         * @param baseJpql 基点となるJPQL (where / order by は含めない)
         * @param staticCondition 条件指定無しに確定する where 条件句 (field is null 等)
         * @param fromIndex 動的に付与する条件句の開始インデックス(1開始)。
         * 既に「field=?1」等で置換連番を付与しているときはその次番号。
         * @param args 既に付与済みの置換連番に紐づく引数
         * @return ビルダー情報
         */
        fun of(baseJpql: String, staticCondition: String, fromIndex: Int, vararg args: Any): JpqlBuilder =
                JpqlBuilder(StringBuilder(baseJpql), AtomicInteger(fromIndex))
                        .add(staticCondition)
                        .reservedArgs(args)
    }

}