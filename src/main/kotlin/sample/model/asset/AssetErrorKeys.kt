package sample.model.asset

/**
 * 資産の審査例外で用いるメッセージキー定数。
 */
interface AssetErrorKeys {
    companion object {
        /** 受渡日を迎えていないため実現できません  */
        const val CashflowRealizeDay = "error.Cashflow.realizeDay"
        /** 既に受渡日を迎えています  */
        const val CashflowBeforeEqualsDay = "error.Cashflow.beforeEqualsDay"

        /** 未到来の受渡日です  */
        const val CashInOutAfterEqualsDay = "error.CashInOut.afterEqualsDay"
        /** 既に発生日を迎えています  */
        const val CashInOutBeforeEqualsDay = "error.CashInOut.beforeEqualsDay"
        /** 出金可能額を超えています  */
        const val CashInOutWithdrawAmount = "error.CashInOut.withdrawAmount"
    }
}