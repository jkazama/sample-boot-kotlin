package sample.context.report.csv

import org.apache.commons.lang3.StringUtils
import java.util.ArrayList
import org.apache.commons.lang3.StringUtils.trimToEmpty
import org.apache.tomcat.util.buf.B2CConverter.getCharset
import sample.context.report.csv.CsvReader.CsvReadLine
import sample.InvocationException
import java.io.*


/**
 * CSVの読込処理をサポートするユーティリティです。
 */
class CsvReader(val data: ByteArray?, val ins: InputStream?, val layout: CsvLayout = CsvLayout()) {

    /** バイナリリソース経由での読み込み時にtrue */
    val fromBinary: Boolean = data != null

    /**
     * CSV読込処理を行います。
     *
     * 大量データ処理を想定してメモリ内に全展開するのではなく、Iteratorを用いた
     * 行処理形式を利用しています。
     * @param logic
     */
    fun read(logic: CsvReadLine) {
        var ins: InputStream? = null
        try {
            ins = if (fromBinary) ByteArrayInputStream(data!!) else this.ins
            readStream(ins, logic)
        } catch (e: RuntimeException) {
            throw e
        } catch (e: Exception) {
            throw InvocationException("リソース処理中に例外が発生しました", e)
        } finally {
            if (fromBinary) {
                closeQuietly(ins)
            }
        }
    }

    private fun closeQuietly(closeable: Closeable?) {
        try {
            closeable?.close()
        } catch (ioe: IOException) {
        }
    }

    fun readStream(`in`: InputStream?, logic: CsvReadLine) {
        val reader = PushbackReader(InputStreamReader(`in`, layout.charset), 2)
        try {
            var lineNum = 0
            var title = false
            while (hasNext(`in`!!, reader)) {
                lineNum++
                val row = readStreamLine(reader) // ヘッダ定義でも行を読み込み、シークを先に進める
                if (lineNum == 1 && layout.header.isNullOrBlank()) {
                    title = true
                    continue // ヘッダ定義存在時は最初の行をスキップ
                }
                logic.execute(if (title) lineNum - 1 else lineNum, row)
            }
        } finally {
            closeQuietly(reader)
        }
    }

    /** 行の存在判定を行います  */
    private fun hasNext(`in`: InputStream, reader: PushbackReader): Boolean {
        `in`.available()
        val i = reader.read()
        if (i != -1) {
            reader.unread(i)
            return true
        }
        return false
    }

    /** InputStreamから行文字列を取得してparseLineを実行します  */
    private fun readStreamLine(reader: PushbackReader): List<String> {
        var inQt = false
        val qt = layout.quote
        val eol = layout.eolSymbols
        val sb = StringBuilder()
        var cp = -1
        while (run { cp = nextCodePoint(reader); cp != -1}) {
            sb.appendCodePoint(cp)
            if (qt.toInt() == cp) {
                if (inQt) {
                    var len = 1
                    while (run { cp = nextCodePoint(reader); cp != -1}) {
                        if (qt.toInt() == cp) {// エスケープ
                            len++
                            sb.appendCodePoint(cp)
                        } else { // 終端
                            reader.unread(Character.toChars(cp))
                            break
                        }
                    }
                    if (len % 2 != 0) {
                        inQt = len != 1
                    } else {
                        inQt = true
                    }
                } else if (!layout.nonQuote) {
                    inQt = true
                }
            }
            if (!inQt && sb.toString().endsWith(eol)) { // 行処理
                return parseRow(stripEol(sb))
            }
        }
        return if (sb.length > 0) {
            if (sb.toString().endsWith(eol)) {
                parseRow(stripEol(sb))
            } else {
                parseRow(sb.toString())
            }
        } else ArrayList()
    }

    /** サロゲートペアを考慮した次の文字位置を返します  */
    private fun nextCodePoint(r: PushbackReader): Int {
        val i = r.read()
        if (i == -1) {
            return -1
        }
        val ch = i.toChar()
        if (Character.isHighSurrogate(ch)) {
            val lo = r.read().toChar()
            return if (Character.isLowSurrogate(lo)) {
                Character.toCodePoint(ch, lo)
            } else {
                throw IOException("想定外のサロゲートペアを検出しました。[" + ch.toString() + ", " + lo.toString() + "]")
            }
        }
        return ch.toString().codePointAt(0)
    }

    private fun stripEol(sb: StringBuilder): String {
        return sb.substring(0, sb.length - layout.eolSymbols.length)
    }

    /** CSV文字列を解析して列一覧を返します  */
    fun parseRow(row: String): List<String> {
        val pdelim = layout.delim.toString().codePointAt(0)
        val pquote = layout.quote.toString().codePointAt(0)
        val columns = ArrayList<String>()
        var column = StringBuilder()
        var inQuote = false
        val max = row.codePointCount(0, row.length)
        var i = 0
        while (i < max) {
            val c = row.codePointAt(i)
            if (c == pquote) {
                if (inQuote) {
                    var cnt = 1
                    column.append(Character.toChars(c))
                    var next = row.offsetByCodePoints(i, 1)
                    while (next < max) {
                        val c2 = row.codePointAt(next)
                        if (c2 != pquote) {
                            break
                        } else {
                            column.append(Character.toChars(c2))
                            cnt++
                            i = next
                        }
                        next = row.offsetByCodePoints(next, 1)
                    }
                    if (cnt % 2 != 0) {
                        inQuote = false
                    }
                } else if (!layout.nonQuote) {
                    inQuote = true
                    column.append(Character.toChars(c))
                } else {
                    column.append(Character.toChars(c))
                }
            } else if (c == pdelim && !inQuote) { // 列切替
                columns.add(unescape(StringUtils.trimToEmpty(column.toString())))
                column = StringBuilder()
                inQuote = false
            } else { // 末尾追記
                column.append(Character.toChars(c))
            }
            i = row.offsetByCodePoints(i, 1)
        }
        columns.add(unescape(StringUtils.trimToEmpty(column.toString())))
        return columns
    }

    private fun unescape(input: String): String {
        if (StringUtils.isBlank(input) || layout.nonQuote) {
            return input
        }
        val delim = layout.delim
        val quote = layout.quote
        val quoteStr = quote.toString()
        val eolStr = layout.eolSymbols
        val eols = ArrayList<String>(eolStr.length)
        var i = 0
        val n = eolStr.codePointCount(0, eolStr.length)
        while (i < n) {
            eols.add(String(Character.toChars(eolStr.codePointAt(i))))
            i++
        }
        if (input[0] != quote || input[input.length - 1] != quote) {
            return input
        }
        val quoteless = input.subSequence(1, input.length - 1).toString()
        val unescape: String
        var eolsAny = false
        for (eol in eols) {
            if (StringUtils.containsAny(quoteless, eol)) {
                eolsAny = true
                break
            }
        }
        if (StringUtils.containsAny(quoteless, delim, quote) || eolsAny) {
            unescape = StringUtils.replace(quoteless, quoteStr + quoteStr, quoteStr)
        } else {
            unescape = input
        }
        val q1 = unescape.indexOf(quote)
        val q2 = unescape.lastIndexOf(quoteStr)
        return if (q1 != q2 && q1 == 0 && unescape.endsWith(quoteStr)) {
            unescape.substring(1, q2)
        } else unescape
    }

    /** 行レベルのCSV読込処理を表現します。   */
    interface CsvReadLine {
        /**
         * @param lineNum 実行行番号(1開始)
         * @param cols 解析された列一覧
         */
        fun execute(lineNum: Int, cols: List<String>)
    }

    companion object {
        fun of(data: ByteArray, layout: CsvLayout = CsvLayout()): CsvReader =
                CsvReader(data, null, layout)

        fun of(ins: InputStream, layout: CsvLayout = CsvLayout()): CsvReader =
                CsvReader(null, ins, layout)
    }

}