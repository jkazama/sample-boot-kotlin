package sample.context.orm

/**
 * ページング一覧を表現します。
 *
 * @param <T> 結果オブジェクト(一覧の要素)
 */
data class PagingList<T>(val list: List<T>, val page: Pagination)