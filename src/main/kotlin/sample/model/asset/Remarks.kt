package sample.model.asset

/**
 * 摘要定数インターフェース。
 */
interface Remarks {
    companion object {
        /** 振込入金  */
        const val CashIn = "cashIn"
        /** 振込入金(調整)  */
        const val CashInAdjust = "cashInAdjust"
        /** 振込入金(取消)  */
        const val CashInCancel = "cashInCancel"
        /** 振込出金  */
        const val CashOut = "cashOut"
        /** 振込出金(調整)  */
        const val CashOutAdjust = "cashOutAdjust"
        /** 振込出金(取消)  */
        const val CashOutCancel = "cashOutCancel"
    }
}
