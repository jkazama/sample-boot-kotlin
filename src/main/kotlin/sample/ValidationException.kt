package sample

import java.io.Serializable

/**
 * Represents validation exception. <p>ValidationException is a recoverable validation exception
 * such as input exception or state transition exception. Due to its nature, output in logs is
 * performed at WARN level (not ERROR). <p>Validation exceptions can be held in multiple
 * global/field scopes. When handling multiple exceptions, please initialize using Warns.
 */
class ValidationException : RuntimeException {
    val warns: Warns
    val list: List<Warn>
        get() = warns.list

    constructor(warns: Warns) : super(warns.head()?.message) {
        this.warns = warns
    }

    constructor(
            message: String,
            field: String? = null,
            messageArgs: Array<String> = arrayOf()
    ) : super(message) {
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

        fun init(
                message: String? = null,
                field: String? = null,
                messageArgs: Array<String> = arrayOf()
        ): Warns =
                when {
                    message.isNullOrBlank() -> Warns()
                    else -> Warns().add(message.orEmpty(), field, messageArgs)
                }
    }
}

class Warn(
        /** Validation exception message */
        val message: String,
        /** Validation exception field key */
        val field: String? = null,
        /** Validation exception message arguments */
        val messageArgs: Array<String> = arrayOf()
) : Serializable {
    /** True when global exception not dependent on field */
    val global: Boolean = field.isNullOrBlank()

    companion object {
        private const val serialVersionUID: Long = 1
    }
}

/** Message key constants used in validation exceptions */
interface ErrorKeys {
    companion object {
        /** A problem may have occurred on the server side */
        const val Exception = "error.Exception"
        /** Information was not found */
        const val EntityNotFound = "error.EntityNotFoundException"
        /** Login status is not valid */
        const val Authentication = "error.Authentication"
        /** Use of the target function is not permitted */
        const val AccessDenied = "error.AccessDeniedException"

        /** Login failed */
        const val Login = "error.login"
        /** This ID is already registered */
        const val DuplicateId = "error.duplicateId"

        /** This information has already been processed */
        const val ActionUnprocessing = "error.ActionStatusType.unprocessing"
    }
}
