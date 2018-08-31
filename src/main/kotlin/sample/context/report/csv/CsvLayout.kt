package sample.context.report.csv

import java.util.ArrayList


/**
 * CSVレイアウトを表現します。
 */
data class CsvLayout(
        /** 区切り文字 */
        val delim: Char = ',',
        /** クオート文字 */
        val quote: Char = '"',
        /** クオート文字を付与しない時はtrue */
        val nonQuote: Boolean = false,
        /** 改行文字 */
        val eolSymbols: String = "\r\n",
        /** ヘッダ文字列 */
        val header: String? = null,
        /** 文字エンコーディング */
        val charset: String = "UTF-8"
) {
    val hasHeader: Boolean = header.isNullOrBlank()

    fun headerCols(): List<String> =
            header.orEmpty().splitToSequence(delim).map { it.trim() }.toList()

}