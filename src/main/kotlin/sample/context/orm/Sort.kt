package sample.context.orm

import sample.context.Dto
import java.io.Serializable

/**
 * ソート情報を表現します。
 * 複数件のソート情報(SortOrder)を内包します。
 */
data class Sort(
        /** ソート条件 */
        val orders: MutableList<SortOrder> = mutableListOf()
) : Dto {

    /** ソート条件を追加します。  */
    fun add(order: SortOrder): Sort {
        orders.add(order)
        return this
    }

    /** ソート条件(昇順)を追加します。  */
    fun asc(property: String): Sort = add(SortOrder.asc(property))
    /** ソート条件(降順)を追加します。  */
    fun desc(property: String): Sort = add(SortOrder.desc(property))

    /** ソート条件が未指定だった際にソート順が上書きされます。  */
    fun ifEmpty(vararg items: SortOrder): Sort {
        if (orders.isEmpty() && items.isNotEmpty()) {
            orders.addAll(items)
        }
        return this
    }

    companion object {
        private const val serialVersionUID: Long = 1

        /** 昇順でソート情報を返します。  */
        fun ascBy(property: String): Sort = Sort().asc(property)
        /** 降順でソート情報を返します。  */
        fun descBy(property: String): Sort = Sort().desc(property)
    }
}

data class SortOrder(
        val property: String,
        val ascending: Boolean
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1

        fun asc(property: String) = SortOrder(property, true)
        fun desc(property: String) = SortOrder(property, false)
    }
}
