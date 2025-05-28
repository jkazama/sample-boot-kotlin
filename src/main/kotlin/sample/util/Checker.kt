package sample.util

/** Represents a simple input checker. */
object Checker {
        /**
         * Whether a string matches a regular expression. (null is allowed)
         *
         * It is recommended to use Regex constants for the regex argument.
         */
        fun match(regex: String, v: Any?): Boolean = v?.toString()?.matches(regex.toRegex()) ?: true

        /** Character length check, returns true when max or less. (Surrogate pair compatible) */
        fun len(v: String, max: Int): Boolean = wordSize(v) <= max

        private fun wordSize(v: String): Int = v.codePointCount(0, v.length)
}
