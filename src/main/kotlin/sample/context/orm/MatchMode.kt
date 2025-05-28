package sample.context.orm

/** Hibernate 6でMatchModeが削除されたため、その代替実装 */
enum class MatchMode {
    EXACT {
        override fun toMatchString(pattern: String): String = pattern
    },
    START {
        override fun toMatchString(pattern: String): String = "$pattern%"
    },
    END {
        override fun toMatchString(pattern: String): String = "%$pattern"
    },
    ANYWHERE {
        override fun toMatchString(pattern: String): String = "%$pattern%"
    };

    abstract fun toMatchString(pattern: String): String
}
