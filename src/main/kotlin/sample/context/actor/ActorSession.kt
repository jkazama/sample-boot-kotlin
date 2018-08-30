package sample.context.actor

import org.springframework.stereotype.Component

/**
 * スレッドローカルスコープの利用者セッション。
 */
@Component
class ActorSession() {

    /** 利用者セッションへ利用者を紐付けます。  */
    fun bind(actor: Actor): ActorSession {
        actorLocal.set(actor)
        return this
    }

    /** 利用者セッションを破棄します。  */
    fun unbind(): ActorSession {
        actorLocal.remove()
        return this
    }

    /** 有効な利用者を返します。紐付けされていない時は匿名者が返されます。  */
    fun actor(): Actor {
        val actor = actorLocal.get()
        return actor ?: Actor.Anonymous
    }

    companion object {
        private val actorLocal = ThreadLocal<Actor>()
    }

}