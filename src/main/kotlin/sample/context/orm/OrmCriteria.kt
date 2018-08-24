package sample.context.orm

import org.hibernate.criterion.MatchMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import javax.persistence.EntityManager
import javax.persistence.criteria.*
import javax.persistence.metamodel.Metamodel


/**
 * ORM の可変条件を取り扱う CriteriaBuilder ラッパー。
 * <p>Criteria の簡易的な取り扱いを可能にします。
 * <p>Criteria で利用する条件句は必要に応じて追加してください。
 * <p>ビルド結果としての CriteriaQuery は result* メソッドで受け取って下さい。
 */
class OrmCriteria<T>(
        val entityClass: Class<T>,
        val alias: String,
        val metamodel: Metamodel?,
        val builder: CriteriaBuilder,
        val query: CriteriaQuery<T>,
        val root: Root<T>,
        val predicates: MutableSet<Predicate> = mutableSetOf(),
        val orders: MutableSet<Order> = mutableSetOf()
) {

    val emptySort: Boolean = !orders.isEmpty()

    /**
     * 関連付けを行います。
     * 引数にはJoin可能なフィールド(@ManyToOne 等)を指定してください。
     * Join した要素は呼び出し元で保持して必要に応じて利用してください。
     */
    fun <Y> join(associationPath: String, alias: String? = null): Join<T, Y> {
        val v: Join<T, Y> = root.join(associationPath)
        if (!alias.isNullOrBlank()) {
            v.alias(alias)
        }
        return v
    }

    /**
     * 組み上げた CriteriaQuery を返します。
     * 複雑なクエリや集計関数は本メソッドで返却された query を元に追加構築してください。
     */
    fun result(): CriteriaQuery<T> {
        return result({ it })
    }

    fun result(extension: (CriteriaQuery<*>) -> CriteriaQuery<*>): CriteriaQuery<T> {
        var q: CriteriaQuery<T> = query.where(*predicates.toTypedArray())
        q = extension(q) as CriteriaQuery<T>
        return if (orders.isEmpty()) q else q.orderBy(*orders.toTypedArray())
    }

    fun resultCount(): CriteriaQuery<Long> =
            resultCount({ it })

    fun resultCount(extension: (CriteriaQuery<*>) -> CriteriaQuery<*>): CriteriaQuery<Long> {
        val q = builder.createQuery(Long::class.java)
        q.from(entityClass).alias(alias)
        q.where(*predicates.toTypedArray())
        if (q.isDistinct) {
            q.select(builder.countDistinct(root))
        } else {
            q.select(builder.count(root))
        }
        return extension(q) as CriteriaQuery<Long>
    }

    /**
     * 条件句 ( or 条件含む ) を追加します。
     *
     * 引数には CriteriaBuilder で生成した Predicate を追加してください。
     */
    fun add(predicate: Predicate): OrmCriteria<T> {
        this.predicates.add(predicate)
        return this
    }

    /** or 条件を付与します。  */
    fun or(vararg predicates: Predicate): OrmCriteria<T> {
        if (predicates.isNotEmpty()) {
            add(builder.or(*predicates))
        }
        return this
    }

    /** null 一致条件を付与します。  */
    fun isNull(field: String): OrmCriteria<T> {
        val path: Path<Any> = root.get(field)
        return add(builder.isNull(path))
    }

    /** null 不一致条件を付与します。  */
    fun isNotNull(field: String): OrmCriteria<T> {
        val path: Path<Any> = root.get(field)
        return add(builder.isNotNull(path))
    }

    /** 一致条件を付与します。( 値が null の時は無視されます )  */
    fun equal(field: String, value: Any?): OrmCriteria<T> =
            equal(root, field, value)

    fun equal(path: Path<*>, field: String, value: Any?): OrmCriteria<T> {
        if (isValid(value)) {
            val fieldPath: Path<Any> = path.get(field)
            add(builder.equal(fieldPath, value))
        }
        return this
    }

    private fun isValid(value: Any?): Boolean =
            when (value) {
                is String? -> !value.isNullOrBlank()
                is Optional<*> -> value.isPresent()
                else -> value != null
            }

    /** 不一致条件を付与します。(値がnullの時は無視されます)  */
    fun equalNot(field: String, value: Any?): OrmCriteria<T> {
        if (isValid(value)) {
            val path: Path<Any> = root.get(field)
            add(builder.notEqual(path, value))
        }
        return this
    }

    /** 一致条件を付与します。(値がnullの時は無視されます)  */
    fun equalProp(field: String, fieldOther: String): OrmCriteria<T> {
        val path: Path<Any> = root.get(field)
        val pathOther: Path<Any> = root.get(fieldOther)
        add(builder.equal(path, pathOther))
        return this
    }

    /** like条件を付与します。(値がnullの時は無視されます)  */
    fun like(field: String, value: String?, mode: MatchMode): OrmCriteria<T> {
        if (isValid(value)) {
            add(builder.like(root.get(field), mode.toMatchString(value)))
        }
        return this
    }

    /** like条件を付与します。[複数フィールドに対するOR結合](値がnullの時は無視されます)  */
    fun like(fields: Array<String>, value: String?, mode: MatchMode): OrmCriteria<T> {
        if (isValid(value)) {
            val predicates = arrayOfNulls<Predicate>(fields.size)
            for (i in fields.indices) {
                predicates[i] = builder.like(root.get(fields[i]), mode.toMatchString(value))
            }
            add(builder.or(*predicates))
        }
        return this
    }

    /** in条件を付与します。  */
    fun `in`(field: String, values: Array<Any>?): OrmCriteria<T> {
        if (values != null && values.isNotEmpty()) {
            val path: Path<Any> = root.get(field)
            add(path.`in`(values))
        }
        return this
    }

    /** between条件を付与します。  */
    fun between(field: String, from: Date?, to: Date?): OrmCriteria<T> {
        if (from != null && to != null) {
            predicates.add(builder.between(root.get(field), from, to))
        } else if (from != null) {
            gte(field, from)
        } else if (to != null) {
            lte(field, to)
        }
        return this
    }

    /** between条件を付与します。  */
    fun between(field: String, from: LocalDate?, to: LocalDate?): OrmCriteria<T> {
        if (from != null && to != null) {
            predicates.add(builder.between(root.get(field), from, to))
        } else if (from != null) {
            gte(field, from)
        } else if (to != null) {
            lte(field, to)
        }
        return this
    }

    /** between条件を付与します。  */
    fun between(field: String, from: LocalDateTime?, to: LocalDateTime?): OrmCriteria<T> {
        if (from != null && to != null) {
            predicates.add(builder.between(root.get(field), from, to))
        } else if (from != null) {
            gte(field, from)
        } else if (to != null) {
            lte(field, to)
        }
        return this
    }

    /** between条件を付与します。  */
    fun between(field: String, from: String, to: String): OrmCriteria<T> {
        if (isValid(from) && isValid(to)) {
            predicates.add(builder.between(root.get(field), from, to))
        } else if (isValid(from)) {
            gte(field, from)
        } else if (isValid(to)) {
            lte(field, to)
        }
        return this
    }

    /** [フィールド]&gt;=[値] 条件を付与します。(値がnullの時は無視されます)  */
    fun <Y : Comparable<Y>> gte(field: String, value: Y?): OrmCriteria<T> {
        if (isValid(value)) {
            add(builder.greaterThanOrEqualTo(root.get(field), value!!))
        }
        return this
    }

    /** [フィールド]&gt;[値] 条件を付与します。(値がnullの時は無視されます)  */
    fun <Y : Comparable<Y>> gt(field: String, value: Y?): OrmCriteria<T> {
        if (isValid(value)) {
            add(builder.greaterThan(root.get(field), value!!))
        }
        return this
    }

    /** [フィールド]&lt;=[値] 条件を付与します。  */
    fun <Y : Comparable<Y>> lte(field: String, value: Y?): OrmCriteria<T> {
        if (isValid(value)) {
            add(builder.lessThanOrEqualTo(root.get(field), value!!))
        }
        return this
    }

    /** [フィールド]&lt;[値] 条件を付与します。  */
    fun <Y : Comparable<Y>> lt(field: String, value: Y?): OrmCriteria<T> {
        if (isValid(value)) {
            add(builder.lessThan(root.get(field), value!!))
        }
        return this
    }

    /** ソート条件を加えます。  */
    fun sort(sort: Sort): OrmCriteria<T> {
        sort.orders.forEach { order -> sort(order) }
        return this
    }

    /** ソート条件を加えます。  */
    fun sort(order: SortOrder): OrmCriteria<T> {
        if (order.ascending) {
            sort(order.property)
        } else {
            sortDesc(order.property)
        }
        return this
    }

    /** 昇順条件を加えます。  */
    fun sort(field: String): OrmCriteria<T> {
        val path: Path<Any> = root.get(field)
        orders.add(builder.asc(path))
        return this
    }

    /** 降順条件を加えます。  */
    fun sortDesc(field: String): OrmCriteria<T> {
        val path: Path<Any> = root.get(field)
        orders.add(builder.desc(path))
        return this
    }

    companion object {
        const val DefaultAlias = "m"

        /** 指定した Entity クラスを軸にしたCriteriaを生成します。  */
        fun <T> of(em: EntityManager, entityClass: Class<T>): OrmCriteria<T> =
                of(em, entityClass, DefaultAlias)

        /** 指定した Entity クラスにエイリアスを紐付けたCriteriaを生成します。  */
        fun <T> of(em: EntityManager, entityClass: Class<T>, alias: String): OrmCriteria<T> {
            val builder = em.criteriaBuilder
            val query = builder.createQuery(entityClass)
            val root = query.from(entityClass)
            root.alias(alias)
            return OrmCriteria(entityClass, alias, em.metamodel, builder, query, root)
        }

    }
}