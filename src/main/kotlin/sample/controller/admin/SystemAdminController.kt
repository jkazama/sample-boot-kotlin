package sample.controller.admin

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import sample.context.AppSetting
import sample.context.FindAppSetting
import sample.context.audit.AuditActor
import sample.context.audit.AuditEvent
import sample.context.audit.FindAuditActor
import sample.context.audit.FindAuditEvent
import sample.context.orm.PagingList
import sample.usecase.admin.SystemAdminService
import javax.validation.Valid

/**
 * システムに関わる社内のUI要求を処理します。
 */
@RestController
@RequestMapping("/api/admin/system")
class SystemAdminController(val service: SystemAdminService) {

    /** 利用者監査ログを検索します。  */
    @GetMapping("/audit/actor/")
    fun findAuditActor(@Valid p: FindAuditActor): PagingList<AuditActor> =
            service.findAuditActor(p)

    /** イベント監査ログを検索します。  */
    @GetMapping("/audit/event/")
    fun findAuditEvent(@Valid p: FindAuditEvent): PagingList<AuditEvent> =
            service.findAuditEvent(p)

    /** アプリケーション設定一覧を検索します。  */
    @GetMapping("/setting/")
    fun findAppSetting(@Valid p: FindAppSetting): List<AppSetting> =
            service.findAppSetting(p)

    /** アプリケーション設定情報を変更します。  */
    @PostMapping("/setting/{id}")
    fun changeAppSetting(@PathVariable id: String, value: String): ResponseEntity<Void> =
            ResponseEntity.ok().apply { service.changeAppSetting(id, value) }.build()

}