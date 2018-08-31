package sample.context.actor

import java.util.*

/**
 * ユースケースにおける利用者を表現します。
 */
data class Actor(
        /** 利用者ID */
        val id: String,
        /** 利用者が持つ{@link ActorRoleType} */
        val roleType: ActorRoleType,
        /** 利用者名称 */
        val name: String = id,
        /** 利用者が使用する{@link Locale} */
        val locale: Locale = Locale.getDefault(),
        /** 利用者の接続チャネル名称 */
        var channel: String? = null,
        /** 利用者を特定する外部情報。(IPなど) */
        var source: String? = null) {
    companion object {
        /** 匿名利用者定数 */
        val Anonymous: Actor = Actor(id = "unknown", roleType = ActorRoleType.Anonymous)
        /** システム利用者定数 */
        val System: Actor = Actor(id = "system", roleType = ActorRoleType.System)
    }
}

/**
 * 利用者の役割を表現します。
 */
enum class ActorRoleType {
    /** 匿名利用者(ID等の特定情報を持たない利用者) */
    Anonymous,
    /** 利用者(主にBtoCの顧客, BtoB提供先社員) */
    User,
    /** 内部利用者(主にBtoCの社員, BtoB提供元社員) */
    Internal,
    /** システム管理者(ITシステム担当社員またはシステム管理会社の社員) */
    Administrator,
    /** システム(システム上の自動処理) */
    System;

    val isAnonymous: Boolean
        get() = this == Anonymous
    val isSystem: Boolean
        get() = this == System
    val notSystem: Boolean = !isSystem
}
