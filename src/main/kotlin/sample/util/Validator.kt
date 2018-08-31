package sample.util

import sample.ValidationException
import sample.Warns
import java.util.function.Consumer

/**
 * 審査例外の構築概念を表現します。
 */
class Validator(val warns: Warns = Warns.init()) {

    /** 審査を行います。validがfalseの時に例外を内部にスタックします。  */
    fun check(valid: Boolean, message: String): Validator {
        if (!valid) {
            warns.add(message)
        }
        return this
    }

    /** 個別属性の審査を行います。validがfalseの時に例外を内部にスタックします。  */
    fun checkField(valid: Boolean, field: String, message: String): Validator {
        if (!valid) {
            warns.add(message, field)
        }
        return this
    }

    /** 審査を行います。失敗した時は即時に例外を発生させます。  */
    fun verify(valid: Boolean, message: String): Validator =
        check(valid, message).verify()

    /** 個別属性の審査を行います。失敗した時は即時に例外を発生させます。  */
    fun verifyField(valid: Boolean, field: String, message: String): Validator =
        checkField(valid, field, message).verify()

    /** 検証します。事前に行ったcheckで例外が存在していた時は例外を発生させます。  */
    fun verify(): Validator {
        if (hasWarn()) {
            throw ValidationException(warns)
        }
        return clear()
    }

    /** 審査例外を保有している時はtrueを返します。   */
    fun hasWarn(): Boolean = warns.nonEmpty()

    /** 内部に保有する審査例外を初期化します。  */
    fun clear(): Validator {
        warns.list.clear()
        return this
    }

    companion object {
        /** 審査処理を行います。  */
        fun validate(proc: (Validator) -> Unit) {
            val validator = Validator()
            proc(validator)
            validator.verify()
        }
    }

}