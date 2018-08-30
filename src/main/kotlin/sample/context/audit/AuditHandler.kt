package sample.context.audit

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import sample.InvocationException
import sample.ValidationException
import sample.context.actor.ActorSession
import sample.context.orm.SystemRepository
import java.util.*


/**
 * 利用者監査やシステム監査(定時バッチや日次バッチ等)などを取り扱います。
 * <p>暗黙的な適用を望む場合は、AOPとの連携も検討してください。
 * <p>対象となるログはLoggerだけでなく、システムスキーマの監査テーブルへ書きだされます。
 * (開始時と完了時で別TXにする事で応答無し状態を検知可能)
 */
@Component
class AuditHandler(
        val session: ActorSession,
        val persister: AuditPersister
) {

    /** 与えた処理に対し、監査ログを記録します。  */
    fun <T> audit(message: String, callable: () -> T): T {
        return audit<T>("default", message, callable)
    }

    /** 与えた処理に対し、監査ログを記録します。  */
    fun audit(message: String, command: Runnable) {
        audit("default", message) {
            command.run()
            true
        }
    }

    /** 与えた処理に対し、監査ログを記録します。  */
    fun <T> audit(category: String, message: String, callable: () -> T): T {
        logger().trace(message(message, "[開始]"))
        val start = System.currentTimeMillis()
        try {
            val v = if (session.actor().roleType.isSystem)
                callEvent(category, message, callable)
            else
                callAudit<T>(category, message, callable)
            logger().info(message(message, "[完了]", start))
            return v
        } catch (e: ValidationException) {
            logger().warn(message(message, "[審例]", start))
            throw e
        } catch (e: RuntimeException) {
            logger().error(message(message, "[例外]", start))
            throw e
        } catch (e: Exception) {
            logger().error(message(message, "[例外]", start))
            throw InvocationException("error.Exception", e)
        }
    }

    /** 与えた処理に対し、監査ログを記録します。  */
    fun audit(category: String, message: String, command: Runnable) {
        audit(category, message) {
            command.run()
            true
        }
    }

    private fun logger(): Logger {
        return if (session.actor().roleType.isSystem) LoggerEvent else LoggerActor
    }

    private fun message(message: String, prefix: String, startMillis: Long? = null): String {
        val actor = session.actor()
        val sb = StringBuilder("$prefix ")
        if (actor.roleType.notSystem) {
            sb.append("[" + actor.id + "] ")
        }
        sb.append(message)
        if (startMillis != null) {
            sb.append(" [" + (System.currentTimeMillis() - startMillis) + "ms]")
        }
        return sb.toString()
    }

    fun <T> callAudit(category: String, message: String, callable: () -> T): T {
        var audit: Optional<AuditActor> = Optional.empty()
        try {
            try { // システムスキーマの障害は本質的なエラーに影響を与えないように
                audit = Optional.of(persister.start(RegAuditActor.of(category, message)))
            } catch (e: Exception) {
                log.error(e.message, e)
            }

            val v = callable()
            try {
                audit.ifPresent { persister.finish(it) }
            } catch (e: Exception) {
                log.error(e.message, e)
            }

            return v
        } catch (e: ValidationException) {
            try {
                audit.ifPresent { persister.cancel(it, e.message.orEmpty()) }
            } catch (ex: Exception) {
                log.error(ex.message, ex)
            }
            throw e
        } catch (e: RuntimeException) {
            try {
                audit.ifPresent { persister.error(it, e.message.orEmpty()) }
            } catch (ex: Exception) {
                log.error(ex.message, ex)
            }
            throw e
        } catch (e: Exception) {
            try {
                audit.ifPresent { persister.error(it, e.message.orEmpty()) }
            } catch (ex: Exception) {
                log.error(ex.message, ex)
            }
            throw InvocationException(e)
        }

    }

    fun <T> callEvent(category: String, message: String, callable: () -> T): T {
        var audit: Optional<AuditEvent> = Optional.empty()
        try {
            try { // システムスキーマの障害は本質的なエラーに影響を与えないように
                audit = Optional.of(persister.start(RegAuditEvent.of(category, message)))
            } catch (e: Exception) {
                log.error(e.message, e)
            }
            val v = callable()
            try {
                audit.ifPresent({ persister.finish(it) })
            } catch (e: Exception) {
                log.error(e.message, e)
            }
            return v
        } catch (e: ValidationException) {
            try {
                audit.ifPresent({ persister.cancel(it, e.message.orEmpty()) })
            } catch (ex: Exception) {
                log.error(ex.message, ex)
            }
            throw e
        } catch (e: RuntimeException) {
            try {
                audit.ifPresent({ persister.error(it, e.message.orEmpty()) })
            } catch (ex: Exception) {
                log.error(ex.message, ex)
            }
            throw e
        } catch (e: Exception) {
            try {
                audit.ifPresent({ persister.error(it, e.message.orEmpty()) })
            } catch (ex: Exception) {
                log.error(ex.message, ex)
            }
            throw InvocationException(e)
        }

    }

    companion object {
        val LoggerActor: Logger = LoggerFactory.getLogger("Audit.Actor")
        val LoggerEvent: Logger = LoggerFactory.getLogger("Audit.Event")
        val log: Logger = LoggerFactory.getLogger(AuditHandler::class.java)
    }

}

/**
 * 監査ログをシステムスキーマへ永続化します。
 */
@Component
class AuditPersister(
        private val rep: SystemRepository
) {

    @Transactional(value = SystemRepository.BeanNameTx, propagation = Propagation.REQUIRES_NEW)
    fun start(p: RegAuditActor): AuditActor =
            AuditActor.register(rep, p)

    @Transactional(value = SystemRepository.BeanNameTx, propagation = Propagation.REQUIRES_NEW)
    fun finish(audit: AuditActor): AuditActor =
            audit.finish(rep)

    @Transactional(value = SystemRepository.BeanNameTx, propagation = Propagation.REQUIRES_NEW)
    fun cancel(audit: AuditActor, errorReason: String): AuditActor =
            audit.cancel(rep, errorReason)

    @Transactional(value = SystemRepository.BeanNameTx, propagation = Propagation.REQUIRES_NEW)
    fun error(audit: AuditActor, errorReason: String): AuditActor =
            audit.error(rep, errorReason)

    @Transactional(value = SystemRepository.BeanNameTx, propagation = Propagation.REQUIRES_NEW)
    fun start(p: RegAuditEvent): AuditEvent =
            AuditEvent.register(rep, p)

    @Transactional(value = SystemRepository.BeanNameTx, propagation = Propagation.REQUIRES_NEW)
    fun finish(event: AuditEvent): AuditEvent =
            event.finish(rep)

    @Transactional(value = SystemRepository.BeanNameTx, propagation = Propagation.REQUIRES_NEW)
    fun cancel(event: AuditEvent, errorReason: String): AuditEvent =
            event.cancel(rep, errorReason)

    @Transactional(value = SystemRepository.BeanNameTx, propagation = Propagation.REQUIRES_NEW)
    fun error(event: AuditEvent, errorReason: String): AuditEvent =
            event.error(rep, errorReason)

}
