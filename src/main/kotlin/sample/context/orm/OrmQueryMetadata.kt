package sample.context.orm

import java.util.*
import javax.persistence.LockModeType

/**
 * Query 向けの追加メタ情報を構築します。
 */
data class OrmQueryMetadata(
        val hints: MutableMap<String, Any> = mutableMapOf(),
        var lockMode: Optional<LockModeType> = Optional.empty()
) {

    /** ヒントを追加します。  */
    fun hint(hintName: String, value: Any): OrmQueryMetadata {
        this.hints[hintName] = value
        return this
    }

    /** ロックモードを設定します。  */
    fun lockMode(lockMode: LockModeType): OrmQueryMetadata {
        this.lockMode = Optional.ofNullable(lockMode)
        return this
    }

    companion object {
        fun empty() = OrmQueryMetadata()
        fun withLock(lockMode: LockModeType) = empty().lockMode(lockMode)
        fun withHint(hintName: String, value: Any) = empty().hint(hintName, value)
    }

}