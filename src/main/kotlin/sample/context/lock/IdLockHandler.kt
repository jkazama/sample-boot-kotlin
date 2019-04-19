package sample.context.lock

import java.io.Serializable
import java.util.concurrent.locks.ReentrantReadWriteLock
import org.apache.logging.log4j.ThreadContext.containsKey
import org.springframework.stereotype.Component
import sample.InvocationException
import java.util.*


/**
 * ID単位のロックを表現します。
 * low: ここではシンプルに口座単位のIDロックのみをターゲットにします。
 * low: 通常はDBのロックテーブルに"for update"要求で悲観的ロックをとったりしますが、サンプルなのでメモリロックにしてます。
 */
@Component
class IdLockHandler() {
    private val lockMap = mutableMapOf<Serializable, ReentrantReadWriteLock>()

    /** IDロック上で処理を実行します。  */
    fun <T> call(id: Serializable, lockType: LockType, callable: () -> T): T {
        if (lockType.isWrite) writeLock(id) else readLock(id)
        try {
            return callable()
        } catch (e: RuntimeException) {
            throw e
        } catch (e: Exception) {
            throw InvocationException("error.Exception", e)
        } finally {
            unlock(id)
        }
    }

    private fun writeLock(id: Serializable) {
        idLock(id).writeLock().lock()
    }

    private fun idLock(id: Serializable): ReentrantReadWriteLock =
        lockMap.computeIfAbsent(id) {
            ReentrantReadWriteLock()
        }

    fun readLock(id: Serializable) {
        idLock(id).readLock().lock()
    }

    fun unlock(id: Serializable) {
        val idLock = idLock(id)
        if (idLock.isWriteLockedByCurrentThread) {
            idLock.writeLock().unlock()
        } else {
            idLock.readLock().unlock()
        }
    }

}

/**
 * ロック種別を表現するEnum。
 */
enum class LockType {
    /** 読み取り専用ロック  */
    Read,
    /** 読み書き専用ロック  */
    Write;

    val isRead: Boolean = !isWrite

    val isWrite: Boolean
        get() = this == Write
}
