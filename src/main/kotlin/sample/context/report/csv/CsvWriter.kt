package sample.context.report.csv

import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils
import java.util.ArrayList
import sample.InvocationException
import java.io.*


/**
 * CSVの書き出し処理をサポートするユーティリティ。
 */
class CsvWriter(val file: File?, val out: OutputStream?, val layout: CsvLayout = CsvLayout()) {

    /** ファイルリソース経由での読み込み時にtrue  */
    val fromFile: Boolean = file != null

    /**
     * CSV書出処理(上書き)を行います。
     *
     * CsvWrite#appendRow 呼び出すタイミングでファイルへ随時書き出しが行われます。
     * @param logic
     */
    fun write(logic: CsvWrite) {
        var out: OutputStream? = null
        try {
            out = if (fromFile) FileUtils.openOutputStream(file) else this.out
            val stream = CsvStream(layout, out!!)
            logic.execute(stream)
        } catch (e: RuntimeException) {
            throw e
        } catch (e: Exception) {
            throw InvocationException(e)
        } finally {
            if (fromFile) {
                closeQuietly(out)
            }
        }
    }

    private fun closeQuietly(closeable: Closeable?) {
        try {
            if (closeable != null) {
                closeable.close()
            }
        } catch (ioe: IOException) {
        }

    }

    /**
     * CSV書出処理(追記)を行います。
     *
     * CsvWrite#appendRow 呼び出すタイミングでファイルへ随時書き出しが行われます。
     *
     * ファイル出力時のみ利用可能です。
     * @param logic
     */
    fun writeAppend(logic: CsvWrite) {
        if (!fromFile)
            throw UnsupportedOperationException("CSV書出処理の追記はファイル出力時のみサポートされます")
        var out: FileOutputStream? = null
        try {
            out = FileUtils.openOutputStream(file, true)
            val stream = CsvStream(layout, out!!)
            logic.execute(stream)
        } catch (e: RuntimeException) {
            throw e
        } catch (e: Exception) {
            throw InvocationException(e)
        } finally {
            closeQuietly(out)
        }
    }

    companion object {
        fun of(file: File, layout: CsvLayout = CsvLayout()): CsvWriter =
                CsvWriter(file, null, layout)
        fun of(out: OutputStream, layout: CsvLayout = CsvLayout()): CsvWriter =
                CsvWriter(null, out, layout)
    }
}

class CsvStream(private val layout: CsvLayout, private val out: OutputStream) {

    init {
        if (layout.hasHeader) {
            appendRow(layout.headerCols())
        }
    }

    fun appendRow(cols: List<Any>): CsvStream {
        try {
            out.write(row(cols).toByteArray(charset(layout.charset)))
            out.write(layout.eolSymbols.toByteArray())
            return this
        } catch (e: RuntimeException) {
            throw e
        } catch (e: Exception) {
            throw IllegalArgumentException(e)
        }

    }

    fun row(cols: List<Any>): String {
        val row = ArrayList<String>()
        for (col in cols) {
            if (col is String) {
                row.add(escape(col.toString()))
            } else {
                row.add(col.toString())
            }
        }
        return StringUtils.join(row, ",")
    }

    private fun escape(s: String): String {
        if (layout.nonQuote) {
            return s
        }
        val delim = layout.delim
        val quote = layout.quote
        val quoteStr = quote.toString()
        val eol = layout.eolSymbols
        return if (StringUtils.containsNone(s, delim, quote) && StringUtils.containsNone(s, eol)) {
            quoteStr + s + quoteStr
        } else {
            quoteStr + StringUtils.replace(s, quoteStr, quoteStr + quoteStr) + quoteStr
        }
    }
}

/** CSV出力処理を表現します。   */
interface CsvWrite {
    /**
     * @param stream 出力CSVインスタンス
     */
    fun execute(stream: CsvStream)
}
