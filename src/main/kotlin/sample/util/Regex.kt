package sample.util

/**
 * 正規表現定数インターフェース。
 * <p>Checker.matchと組み合わせて利用してください。
 */
object Regex {
    /** Ascii  */
    const val rAscii = "^\\p{ASCII}*$"
    /** 英字  */
    const val rAlpha = "^[a-zA-Z]*$"
    /** 英字大文字  */
    const val rAlphaUpper = "^[A-Z]*$"
    /** 英字小文字  */
    const val rAlphaLower = "^[a-z]*$"
    /** 英数  */
    const val rAlnum = "^[0-9a-zA-Z]*$"
    /** シンボル  */
    const val rSymbol = "^\\p{Punct}*$"
    /** 英数記号  */
    const val rAlnumSymbol = "^[0-9a-zA-Z\\p{Punct}]*$"
    /** 数字  */
    const val rNumber = "^[-]?[0-9]*$"
    /** 整数  */
    const val rNumberNatural = "^[0-9]*$"
    /** 倍精度浮動小数点  */
    const val rDecimal = "^[-]?(\\d+)(\\.\\d+)?$"
    // see UnicodeBlock
    /** ひらがな  */
    const val rHiragana = "^\\p{InHiragana}*$"
    /** カタカナ  */
    const val rKatakana = "^\\p{InKatakana}*$"
    /** 半角カタカナ  */
    const val rHankata = "^[｡-ﾟ]*$"
    /** 半角文字列  */
    const val rHankaku = "^[\\p{InBasicLatin}｡-ﾟ]*$" // ラテン文字 + 半角カタカナ
    /** 全角文字列  */
    const val rZenkaku = "^[^\\p{InBasicLatin}｡-ﾟ]*$" // 全角の定義を半角以外で割り切り
    /** 漢字  */
    const val rKanji = "^[\\p{InCJKUnifiedIdeographs}々\\p{InCJKCompatibilityIdeographs}]*$"
    /** 文字  */
    const val rWord = "^(?s).*$"
    /** コード  */
    const val rCode = "^[0-9a-zA-Z_-]*$" // 英数 + アンダーバー + ハイフン
}