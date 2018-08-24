package sample.util

import com.ibm.icu.text.Transliterator
import java.math.BigDecimal
import java.util.*

/** 各種型/文字列変換をサポートします。(ICU4Jライブラリに依存しています) */
object ConvertUtils {
    private val ZenkakuToHan = Transliterator.getInstance("Fullwidth-Halfwidth")
    private val HankakuToZen = Transliterator.getInstance("Halfwidth-Fullwidth")
    private val KatakanaToHira = Transliterator.getInstance("Katakana-Hiragana")
    private val HiraganaToKana = Transliterator.getInstance("Hiragana-Katakana")

    /** 例外無しにLongへ変換します。(変換できない時はnull)  */
    fun quietlyLong(value: Any?): Long? {
        try {
            return Optional.ofNullable(value).map({ java.lang.Long.parseLong(it.toString()) }).orElse(null)
        } catch (e: NumberFormatException) {
            return null
        }

    }

    /** 例外無しにIntegerへ変換します。(変換できない時はnull)  */
    fun quietlyInt(value: Any?): Int? {
        try {
            return Optional.ofNullable(value).map({ Integer.parseInt(it.toString()) }).orElse(null)
        } catch (e: NumberFormatException) {
            return null
        }

    }

    /** 例外無しにBigDecimalへ変換します。(変換できない時はnull)  */
    fun quietlyDecimal(value: Any?): BigDecimal? {
        try {
            return Optional.ofNullable(value).map({ BigDecimal(it.toString()) }).orElse(null)
        } catch (e: NumberFormatException) {
            return null
        }
    }

    /** 例外無しBooleanへ変換します。(変換できない時はfalse)  */
    fun quietlyBool(value: Any?): Boolean? =
        Optional.ofNullable(value).map({ java.lang.Boolean.parseBoolean(it.toString()) }).orElse(false)

    /** 全角文字を半角にします。  */
    fun zenkakuToHan(text: String?): String? =
        Optional.ofNullable(text).map({ ZenkakuToHan.transliterate(it) }).orElse(null)

    /** 半角文字を全角にします。  */
    fun hankakuToZen(text: String?): String? =
        Optional.ofNullable(text).map({ HankakuToZen.transliterate(it) }).orElse(null)

    /** カタカナをひらがなにします。  */
    fun katakanaToHira(text: String?): String? =
        Optional.ofNullable(text).map({ KatakanaToHira.transliterate(it) }).orElse(null)

    /**
     * ひらがな/半角カタカナを全角カタカナにします。
     *
     * low: 実際の挙動は厳密ではないので単体検証(ConvertUtilsTest)などで事前に確認して下さい。
     */
    fun hiraganaToZenKana(text: String?): String? =
        Optional.ofNullable(text).map({ HiraganaToKana.transliterate(it) }).orElse(null)

    /**
     * ひらがな/全角カタカナを半角カタカナにします。
     *
     * low: 実際の挙動は厳密ではないので単体検証(ConvertUtilsTest)などで事前に確認して下さい。
     */
    fun hiraganaToHanKana(text: String?): String? =
        zenkakuToHan(hiraganaToZenKana(text))

    /** 指定した文字列を抽出します。(サロゲートペア対応)  */
    fun substring(text: String?, start: Int, end: Int): String? {
        if (text == null) {
            return null
        }
        val spos = text.offsetByCodePoints(0, start)
        val epos = if (text.length < end) text.length else end
        return text.substring(spos, text.offsetByCodePoints(spos, epos - start))
    }

    /** 文字列を左から指定の文字数で取得します。(サロゲートペア対応)  */
    fun left(text: String?, len: Int): String? =
        substring(text, 0, len)

    /** 文字列を左から指定のバイト数で取得します。  */
    fun leftStrict(text: String?, lenByte: Int, charset: String): String? {
        if (text == null) {
            return null
        }
        val sb = StringBuilder()
        try {
            var cnt = 0
            for (i in 0 until text.length) {
                val v = text.substring(i, i + 1)
                val b = v.toByteArray(charset(charset))
                if (lenByte < cnt + b.size) {
                    break
                } else {
                    sb.append(v)
                    cnt += b.size
                }
            }
        } catch (e: Exception) {
            throw IllegalArgumentException(e)
        }
        return sb.toString()
    }

}