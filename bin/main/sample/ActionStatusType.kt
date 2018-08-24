package sample

/**
 * 何らかの行為に関わる処理ステータス概念。
 */
enum class ActionStatusType {
    /** 未処理 */
    Unprocessed,
    /** 処理中 */
    Processing,
    /** 処理済 */
    Processed,
    /** 取消 */
    Cancelled,
    /** エラー */
    Error;

    fun isFinish(): Boolean = finishTypes.contains(this)
    fun isUnprocessing(): Boolean = unprocessingTypes.contains(this)
    fun isUnprocessed(): Boolean = unprocessedTypes.contains(this)

    companion object {
        /** 完了済みのステータス一覧 */
        val finishTypes: List<ActionStatusType> = listOf(Processed, Cancelled)
        /** 未完了のステータス一覧(処理中は含めない) */
        val unprocessingTypes: List<ActionStatusType> = listOf(Unprocessed, Error)
        /** 未完了のステータス一覧(処理中も含める) */
        val unprocessedTypes: List<ActionStatusType> = listOf(Unprocessed, Processing, Error)
    }
}