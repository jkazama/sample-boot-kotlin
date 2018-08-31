package sample.context.audit

import org.apache.commons.lang3.StringUtils
import org.hibernate.criterion.MatchMode
import org.springframework.format.annotation.DateTimeFormat
import sample.ActionStatusType
import sample.context.Dto
import sample.context.orm.*
import sample.model.constraints.DescriptionEmpty
import sample.model.constraints.NameEmpty
import sample.util.DateUtils
import java.time.LocalDate
import java.time.LocalDateTime
import javax.persistence.*
import javax.validation.constraints.NotNull


/**
 * システムイベントの監査ログを表現します。
 */
@Entity
data class AuditEvent(
        @Id
        @GeneratedValue
        var id: Long? = null,
        /** カテゴリ */
        var category: String? = null,
        /** メッセージ */
        var message: String,
        /** 処理ステータス */
        @Enumerated(EnumType.STRING)
        var statusType: ActionStatusType,
        /** エラー事由 */
        var errorReason: String? = null,
        /** 処理時間(msec) */
        var time: Long? = null,
        /** 開始日時 */
        @field:NotNull
        var startDate: LocalDateTime,
        /** 終了日時(未完了時はnull) */
        var endDate: LocalDateTime? = null
) : OrmActiveRecord<AuditEvent>() {

    /** イベント監査ログを完了状態にします。  */
    fun finish(rep: SystemRepository): AuditEvent {
        val now = rep.dh().time.date()
        this.statusType = ActionStatusType.Processed
        this.endDate = now
        this.time = DateUtils.between(startDate, endDate).get().toMillis()
        return this.update(rep)
    }

    /** イベント監査ログを取消状態にします。  */
    fun cancel(rep: SystemRepository, errorReason: String): AuditEvent {
        val now = rep.dh().time.date()
        this.statusType = ActionStatusType.Cancelled
        this.errorReason = StringUtils.abbreviate(errorReason, 250)
        this.endDate = now
        this.time = DateUtils.between(startDate, endDate).get().toMillis()
        return this.update(rep)
    }

    /** イベント監査ログを例外状態にします。  */
    fun error(rep: SystemRepository, errorReason: String): AuditEvent {
        val now = rep.dh().time.date()
        this.statusType = ActionStatusType.Error
        this.errorReason = StringUtils.abbreviate(errorReason, 250)
        this.endDate = now
        this.time = DateUtils.between(startDate, endDate).get().toMillis()
        return update(rep)
    }

    companion object {
        private const val serialVersionUID = 1L

        /** イベント監査ログを登録します。  */
        fun register(rep: SystemRepository, p: RegAuditEvent): AuditEvent =
                p.create(rep.dh().time.date()).save(rep)

        /** イベント監査ログを検索します。  */
        fun find(rep: SystemRepository, p: FindAuditEvent): PagingList<AuditEvent> =
                rep.tmpl().findByCriteria(AuditEvent::class.java, { criteria ->
                    criteria
                            .equal("category", p.category)
                            .equal("statusType", p.statusType)
                            .like(arrayOf("message", "errorReason"), p.keyword, MatchMode.ANYWHERE)
                            .between("startDate", p.fromDay!!.atStartOfDay(), DateUtils.dateTo(p.toDay!!))
                }, p.page.sortIfEmpty(SortOrder.desc("startDate")))
    }

}

/** 検索パラメタ  */
data class FindAuditEvent(
        @field:NameEmpty
        val category: String? = null,
        @field:DescriptionEmpty
        val keyword: String? = null,
        val statusType: ActionStatusType? = null,
        @field:NotNull
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        val fromDay: LocalDate? = null,
        @field:NotNull
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        val toDay: LocalDate? = null,
        @field:NotNull
        val page: Pagination = Pagination()
) : Dto {

    companion object {
        private const val serialVersionUID = 1L
    }
}

/** 登録パラメタ  */
data class RegAuditEvent(
        @field:NameEmpty
        val category: String? = null,
        val message: String
) : Dto {

    fun create(now: LocalDateTime): AuditEvent =
            AuditEvent(
                    category = category,
                    message = message,
                    statusType = ActionStatusType.Processing,
                    startDate = now)

    companion object {
        private const val serialVersionUID = 1L

        fun of(message: String): RegAuditEvent =
                of("default", message)

        fun of(category: String, message: String): RegAuditEvent =
                RegAuditEvent(category, message)
    }
}

