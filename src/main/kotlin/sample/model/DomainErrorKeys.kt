package sample.model

/**
 * 汎用ドメインで用いるメッセージキー定数。
 */
interface DomainErrorKeys {
    companion object {
        /** マイナスを含めない数字を入力してください  */
        const val AbsAmountZero = "error.domain.AbsAmount.zero"
    }
}