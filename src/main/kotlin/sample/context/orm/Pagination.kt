package sample.context.orm

import sample.context.Dto
import sample.util.Calculator
import java.math.RoundingMode

/**
 * ページング情報を表現します。
 */
data class Pagination(
        val page: Int = 1,
        val size: Int = DefaultSize,
        val total: Long? = null,
        var ignoreTotal: Boolean = false,
        val sort: Sort = Sort()) : Dto {

    /** 最大ページ数を返します。total設定時のみ適切な値が返されます。 */
    val maxPage: Int = if (total == null) 0 else Calculator.of(total).scale(0, RoundingMode.UP).divideBy(size).intValue()
    /** 開始件数を返します。 */
    val firstResult: Int = (page - 1) * size

    /** カウント算出を無効化します。 */
    fun ignoreTotal(): Pagination {
        this.ignoreTotal = true
        return this
    }

    /** ソート指定が未指定の時は与えたソート条件で上書きします。  */
    fun sortIfEmpty(vararg orders: SortOrder): Pagination {
        sort.ifEmpty(*orders)
        return this
    }

    companion object {
        private const val serialVersionUID: Long = 1
        const val DefaultSize: Int = 100

        fun of(req: Pagination, total: Long): Pagination =
                Pagination(page = req.page, size = req.size, total = total, ignoreTotal = false, sort = req.sort)

    }
}