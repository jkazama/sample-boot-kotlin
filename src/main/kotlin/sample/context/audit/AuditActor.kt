package sample.context.audit

import org.apache.commons.lang3.StringUtils
import org.hibernate.criterion.MatchMode
import sample.ActionStatusType
import sample.context.Dto
import sample.context.actor.Actor
import sample.context.actor.ActorRoleType
import sample.context.orm.*
import sample.model.constraints.*
import sample.util.ConvertUtils
import sample.util.DateUtils
import java.time.LocalDate
import java.time.LocalDateTime
import javax.persistence.*
import javax.validation.constraints.NotNull


/**
 * システム利用者の監査ログを表現します。
 */
@Entity
data class AuditActor(
        @Id
        @GeneratedValue
        var id: Long? = null,
        /** 利用者ID */
        @IdStr
        var actorId: String,
        /** 利用者役割 */
        @NotNull
        @Enumerated(EnumType.STRING)
        var roleType: ActorRoleType,
        /** 利用者ソース(IP等) */
        var source: String? = null,
        /** カテゴリ */
        @Category
        var category: String? = null,
        /** メッセージ */
        var message: String,
        /** 処理ステータス */
        @NotNull
        @Enumerated(EnumType.STRING)
        var statusType: ActionStatusType,
        /** エラー事由 */
        @DescriptionEmpty
        var errorReason: String? = null,
        /** 処理時間(msec) */
        var time: Long? = null,
        /** 開始日時 */
        @NotNull
        var startDate: LocalDateTime,
        /** 終了日時(未完了時はnull) */
        var endDate: LocalDateTime? = null
) : OrmActiveRecord<AuditActor>() {

    /** 利用者監査ログを完了状態にします。  */
    fun finish(rep: SystemRepository): AuditActor {
        val now = rep.dh().time.date()
        this.statusType = ActionStatusType.Processed
        this.endDate = now
        this.time = DateUtils.between(startDate, endDate).get().toMillis()
        return this.update(rep)
    }

    /** 利用者監査ログを取消状態にします。  */
    fun cancel(rep: SystemRepository, errorReason: String): AuditActor {
        val now = rep.dh().time.date()
        this.statusType = ActionStatusType.Cancelled
        this.errorReason = StringUtils.abbreviate(errorReason, 250)
        this.endDate = now
        this.time = DateUtils.between(startDate, endDate).get().toMillis()
        return this.update(rep)
    }

    /** 利用者監査ログを例外状態にします。  */
    fun error(rep: SystemRepository, errorReason: String): AuditActor {
        val now = rep.dh().time.date()
        this.statusType = ActionStatusType.Error
        this.errorReason = StringUtils.abbreviate(errorReason, 250)
        this.endDate = now
        this.time = DateUtils.between(startDate, endDate).get().toMillis()
        return update(rep)
    }

    companion object {
        private const val serialVersionUID = 1L

        /** 利用者監査ログを登録します。  */
        fun register(rep: SystemRepository, p: RegAuditActor): AuditActor =
                p.create(rep.dh().actor(), rep.dh().time.date()).save(rep)

        /** 利用者監査ログを検索します。  */
        fun find(rep: SystemRepository, p: FindAuditActor): PagingList<AuditActor> =
                rep.tmpl().findByCriteria(AuditActor::class.java, { criteria ->
                    criteria
                            .like(arrayOf("actorId", "source"), p.actorId, MatchMode.ANYWHERE)
                            .equal("category", p.category)
                            .equal("roleType", p.roleType)
                            .equal("statusType", p.statusType)
                            .like(arrayOf("message", "errorReason"), p.keyword, MatchMode.ANYWHERE)
                            .between("startDate", p.fromDay.atStartOfDay(), DateUtils.dateTo(p.toDay))
                }, p.page.sortIfEmpty(SortOrder.desc("startDate")))
    }

}

/** 検索パラメタ  */
data class FindAuditActor(
        @IdStrEmpty
        val actorId: String? = null,
        @CategoryEmpty
        val category: String? = null,
        @DescriptionEmpty
        val keyword: String? = null,
        @NotNull
        val roleType: ActorRoleType = ActorRoleType.User,
        val statusType: ActionStatusType? = null,
        @ISODate
        val fromDay: LocalDate,
        @ISODate
        val toDay: LocalDate,
        @NotNull
        val page: Pagination = Pagination()
) : Dto {
    companion object {
        private const val serialVersionUID = 1L
    }
}

/** 登録パラメタ  */
data class RegAuditActor(
        val category: String? = null,
        val message: String? = null
) : Dto {
    fun create(actor: Actor, now: LocalDateTime): AuditActor =
            AuditActor(
                    actorId = actor.id,
                    roleType = actor.roleType,
                    source = actor.source,
                    category = category,
                    message = ConvertUtils.left(message, 300).orEmpty(),
                    statusType = ActionStatusType.Processing,
                    startDate = now)

    companion object {
        private val serialVersionUID = 1L

        fun of(message: String): RegAuditActor =
                of("default", message)

        fun of(category: String, message: String): RegAuditActor =
                RegAuditActor(category, message)
    }
}

