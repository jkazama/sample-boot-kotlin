package sample

/**
 * 処理時の実行例外を表現します。
 * <p>復旧不可能なシステム例外をラップする目的で利用してください。
 */
class InvocationException : RuntimeException {
    constructor(message: String, cause: Throwable): super(message, cause) {}
    constructor(message: String): super(message) {}
    constructor(cause: Throwable): super(cause) {}

    companion object {
        private const val serialVersionUID: Long = 1
    }
}