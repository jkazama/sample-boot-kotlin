package sample.util

/**
 * 簡易的な入力チェッカーを表現します。
 */
object Checker {
    /**
     * 正規表現に文字列がマッチするか。(nullは許容)
     *
     * 引数のregexにはRegex定数を利用する事を推奨します。
     */
    fun match(regex: String, v: Any?): Boolean =
            v?.toString()?.matches(regex.toRegex()) ?: true

    /** 文字桁数チェック、max以下の時はtrue。(サロゲートペア対応)  */
    fun len(v: String, max: Int): Boolean =
            wordSize(v) <= max

    private fun wordSize(v: String): Int =
            v.codePointCount(0, v.length)
}