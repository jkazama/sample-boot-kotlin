package sample.util

/** Regular expression constants interface. <p>Please use in combination with Checker.match. */
object Regex {
    /** ASCII */
    const val rAscii = "^\\p{ASCII}*$"
    /** Alphabetic */
    const val rAlpha = "^[a-zA-Z]*$"
    /** Uppercase alphabetic */
    const val rAlphaUpper = "^[A-Z]*$"
    /** Lowercase alphabetic */
    const val rAlphaLower = "^[a-z]*$"
    /** Alphanumeric */
    const val rAlnum = "^[0-9a-zA-Z]*$"
    /** Symbol */
    const val rSymbol = "^\\p{Punct}*$"
    /** Alphanumeric symbol */
    const val rAlnumSymbol = "^[0-9a-zA-Z\\p{Punct}]*$"
    /** Number */
    const val rNumber = "^[-]?[0-9]*$"
    /** Natural number */
    const val rNumberNatural = "^[0-9]*$"
    /** Double precision floating point */
    const val rDecimal = "^[-]?(\\d+)(\\.\\d+)?$"
    // see UnicodeBlock
    /** Hiragana */
    const val rHiragana = "^\\p{InHiragana}*$"
    /** Katakana */
    const val rKatakana = "^\\p{InKatakana}*$"
    /** Half-width Katakana */
    const val rHankata = "^[｡-ﾟ]*$"
    /** Half-width string */
    const val rHankaku = "^[\\p{InBasicLatin}｡-ﾟ]*$" // Latin characters + half-width Katakana
    /** Full-width string */
    const val rZenkaku = "^[^\\p{InBasicLatin}｡-ﾟ]*$" // Full-width definition as non-half-width
    /** Kanji */
    const val rKanji = "^[\\p{InCJKUnifiedIdeographs}々\\p{InCJKCompatibilityIdeographs}]*$"
    /** Character */
    const val rWord = "^(?s).*$"
    /** Code */
    const val rCode = "^[0-9a-zA-Z_-]*$" // Alphanumeric + underscore + hyphen
}
