package sample.context.report

import sample.context.Dto

/** ファイルイメージを表現します。 */
data class ReportFile(val name: String, val data: Array<Byte>): Dto {
    val size: Int = data.size
    companion object {
        private const val serialVersionUID = 1L
    }
}