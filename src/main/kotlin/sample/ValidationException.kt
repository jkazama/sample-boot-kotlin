package sample

import java.io.Serializable

/**
 * 審査例外を表現します。
 * <p>ValidationExceptionは入力例外や状態遷移例外等の復旧可能な審査例外です。
 * その性質上ログ等での出力はWARNレベル(ERRORでなく)で行われます。
 * <p>審査例外はグローバル/フィールドスコープで複数保有する事が可能です。複数件の例外を取り扱う際は
 * Warnsを利用して初期化してください。
 */
class ValidationException : RuntimeException {
    val warns: Warns
    val list: List<Warn>
        get() = warns.list

    constructor(warns: Warns) : super(warns.head()?.message) {
        this.warns = warns
    }

    constructor(message: String, field: String? = null, messageArgs: Array<String> = arrayOf()) : super(message) {
        this.warns = Warns.init(message, field, messageArgs)
    }

    companion object {
        private const val serialVersionUID: Long = 1
    }
}

class Warns(val list: MutableList<Warn> = mutableListOf()) : Serializable {

    fun head(): Warn? = list.firstOrNull()
    fun nonEmpty(): Boolean = !list.isEmpty()

    fun add(message: String, field: String? = null, messageArgs: Array<String> = arrayOf()): Warns {
        list.add(Warn(message, field, messageArgs))
        return this
    }

    companion object {
        private const val serialVersionUID: Long = 1

        fun init(message: String? = null, field: String? = null, messageArgs: Array<String> = arrayOf()): Warns =
                when {
                    message.isNullOrBlank() -> Warns()
                    else -> Warns().add(message.orEmpty(), field, messageArgs)
                }
    }
}

class Warn(
        /** 審査例外メッセージ  */
        val message: String,
        /** 審査例外フィールドキー  */
        val field: String? = null,
        /** 審査例外メッセージ引数  */
        val messageArgs: Array<String> = arrayOf()) : Serializable {
    /** フィールドに従属しないグローバル例外時はtrue  */
    val global: Boolean = field.isNullOrBlank()

    companion object {
        private const val serialVersionUID: Long = 1
    }
}

/** 審査例外で用いるメッセージキー定数  */
interface ErrorKeys {
    companion object {
        /** サーバー側で問題が発生した可能性があります  */
        const val Exception = "error.Exception"
        /** 情報が見つかりませんでした  */
        const val EntityNotFound = "error.EntityNotFoundException"
        /** ログイン状態が有効ではありません  */
        const val Authentication = "error.Authentication"
        /** 対象機能の利用が認められていません  */
        const val AccessDenied = "error.AccessDeniedException"

        /** ログインに失敗しました  */
        const val Login = "error.login"
        /** 既に登録されているIDです  */
        const val DuplicateId = "error.duplicateId"

        /** 既に処理済の情報です  */
        const val ActionUnprocessing = "error.ActionStatusType.unprocessing"
    }
}
