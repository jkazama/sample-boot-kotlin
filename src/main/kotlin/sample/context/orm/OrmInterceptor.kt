package sample.context.orm

import org.springframework.stereotype.Component
import sample.context.Timestamper
import sample.context.actor.ActorSession

/**
 * Entityの永続化タイミングでAOP処理を差し込む Interceptor。
 */
@Component
class OrmInterceptor(
        private val session: ActorSession,
        private val time: Timestamper
) {

    /** 登録時の事前差し込み処理を行います。   */
    fun touchForCreate(entity: Any) {
        if (entity is OrmActiveMetaRecord<*>) {
            val staff = session.actor()
            val now = time.date()
            entity.createId = staff.id
            entity.createDate = now
            entity.updateId = staff.id
            entity.updateDate = now
        }
    }

    /** 変更時の事前差し込み処理を行います。    */
    fun touchForUpdate(entity: Any): Boolean {
        if (entity is OrmActiveMetaRecord<*>) {
            val staff = session.actor()
            val now = time.date()
            if (entity.createDate == null) {
                entity.createId = staff.id
                entity.createDate = now
            }
            entity.updateId = staff.id
            entity.updateDate = now
        }
        return false
    }

}