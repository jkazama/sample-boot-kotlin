package sample.context.report

import org.springframework.stereotype.Component
import sample.context.report.csv.CsvWriter
import sample.context.report.csv.CsvWrite
import sample.context.report.csv.CsvLayout
import sample.context.report.csv.CsvReader
import sample.context.report.csv.CsvReader.CsvReadLine
import sample.InvocationException
import java.io.*


/**
 * 帳票処理を行います。
 * low: サンプルではCSVのみ提供します。実際は固定長/Excel/PDFなどの取込/出力なども取り扱う可能性があります。
 * low: ExcelはPOI、PDFはJasperReportの利用が一般的です。(商用製品を利用するのもおすすめです)
 */
@Component
class ReportHandler {

    /**
     * 帳票をオンメモリ上でbyte配列にします。
     *
     * 大量データ等、パフォーマンス上のボトルネックが無いときはこちらの処理内でレポートを書き出しするようにしてください。
     */
    fun convert(logic: ReportToByte): ByteArray {
        val out = ByteArrayOutputStream()
        try {
            DataOutputStream(out).use {
                logic.execute(out)
                return out.toByteArray()
            }
        } catch (e: IOException) {
            throw InvocationException(e)
        }

    }

    /**
     * CSVファイルを読み込んで行単位に処理を行います。
     * @param data 読み込み対象となるバイナリ
     * @param logic 行単位の読込処理
     */
    fun readCsv(data: ByteArray, logic: CsvReadLine) {
        CsvReader.of(data).read(logic)
    }

    fun readCsv(data: ByteArray, layout: CsvLayout, logic: CsvReadLine) {
        CsvReader.of(data, layout).read(logic)
    }

    /**
     * CSVストリームを読み込んで行単位に処理を行います。
     * @param ins 読み込み対象となるInputStream
     * @param logic 行単位の読込処理
     */
    fun readCsv(ins: InputStream, logic: CsvReadLine) {
        CsvReader.of(ins).read(logic)
    }

    fun readCsv(ins: InputStream, layout: CsvLayout, logic: CsvReadLine) {
        CsvReader.of(ins, layout).read(logic)
    }

    /**
     * CSVファイルを書き出しします。
     * @param file 出力対象となるファイル
     * @param logic 書出処理
     */
    fun writeCsv(file: File, logic: CsvWrite) {
        CsvWriter.of(file).write(logic)
    }

    fun writeCsv(file: File, layout: CsvLayout, logic: CsvWrite) {
        CsvWriter.of(file, layout).write(logic)
    }

    /**
     * CSVストリームに書き出しします。
     * @param out 出力Stream
     * @param logic 書出処理
     */
    fun writeCsv(out: OutputStream, logic: CsvWrite) {
        CsvWriter.of(out).write(logic)
    }

    fun writeCsv(out: OutputStream, layout: CsvLayout, logic: CsvWrite) {
        CsvWriter.of(out, layout).write(logic)
    }

    /** レポートをバイナリ形式で OutputStream へ書き出します。  */
    interface ReportToByte {
        fun execute(out: OutputStream)
    }

}
