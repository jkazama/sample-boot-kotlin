package sample

/** Processing status concept related to some action. */
enum class ActionStatusType {
    /** Unprocessed */
    Unprocessed,
    /** Processing */
    Processing,
    /** Processed */
    Processed,
    /** Cancelled */
    Cancelled,
    /** Error */
    Error;

    val isFinish: Boolean
        get() = finishTypes.contains(this)
    val isUnprocessing: Boolean
        get() = unprocessingTypes.contains(this)
    val isUnprocessed: Boolean
        get() = unprocessedTypes.contains(this)

    companion object {
        /** List of completed statuses */
        val finishTypes: List<ActionStatusType> = listOf(Processed, Cancelled)
        /** List of incomplete statuses (excluding processing) */
        val unprocessingTypes: List<ActionStatusType> = listOf(Unprocessed, Error)
        /** List of incomplete statuses (including processing) */
        val unprocessedTypes: List<ActionStatusType> = listOf(Unprocessed, Processing, Error)
    }
}
