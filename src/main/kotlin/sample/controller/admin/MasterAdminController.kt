package sample.controller.admin

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import sample.ErrorKeys
import sample.ValidationException
import sample.context.security.SecurityActorFinder
import sample.context.security.SecurityProperties
import sample.model.master.RegHoliday
import sample.usecase.admin.MasterAdminService
import javax.validation.Valid

/**
 * マスタに関わる社内のUI要求を処理します。
 */
@RestController
@RequestMapping("/api/admin/master")
class MasterAdminController(
        val service: MasterAdminService,
        val securityProps: SecurityProperties
) {
    /** 社員ログイン状態を確認します。  */
    @GetMapping("/loginStatus")
    fun loginStatus(): Boolean = true

    /** 社員ログイン情報を取得します。  */
    @GetMapping("/loginStaff")
    fun loadLoginStaff(): LoginStaffUI =
            when (securityProps.auth.enabled) {
                true -> {
                    val actorDetails = SecurityActorFinder.actorDetails()
                            .orElseThrow { ValidationException(ErrorKeys.Authentication) }
                    val actor = actorDetails.actor
                    LoginStaffUI(actor.id, actor.name, actorDetails.authorityIds)
                }
                false -> // for dummy login
                    LoginStaffUI("sample", "sample", listOf())
            }

    /** 休日を登録します。  */
    @PostMapping("/holiday/")
    fun registerHoliday(@Valid p: RegHoliday): ResponseEntity<Void> =
            ResponseEntity.ok().apply { service.registerHoliday(p) }.build()

}

/** クライアント利用用途に絞ったパラメタ  */
data class LoginStaffUI(
        val id: String,
        val name: String,
        val authorities: List<String>
)