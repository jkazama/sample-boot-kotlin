package sample.model.asset

/** Remarks constants interface. */
interface Remarks {
    companion object {
        /** Wire transfer deposit */
        const val CashIn = "cashIn"
        /** Wire transfer deposit (adjustment) */
        const val CashInAdjust = "cashInAdjust"
        /** Wire transfer deposit (cancellation) */
        const val CashInCancel = "cashInCancel"
        /** Wire transfer withdrawal */
        const val CashOut = "cashOut"
        /** Wire transfer withdrawal (adjustment) */
        const val CashOutAdjust = "cashOutAdjust"
        /** Wire transfer withdrawal (cancellation) */
        const val CashOutCancel = "cashOutCancel"
    }
}
