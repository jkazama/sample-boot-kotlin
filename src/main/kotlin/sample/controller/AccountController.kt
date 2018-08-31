package sample.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import sample.ErrorKeys
import sample.ValidationException
import sample.context.security.SecurityActorFinder
import sample.context.security.SecurityProperties
import sample.usecase.AccountService


/**
 * 口座に関わる顧客のUI要求を処理します。
 */
@RestController
@RequestMapping("/api/account")
class AccountController(
        val securityProps: SecurityProperties
) {

    /** ログイン状態を確認します。  */
    @GetMapping("/loginStatus")
    fun loginStatus(): ResponseEntity<Boolean> = ResponseEntity.ok(true)

    /** 口座ログイン情報を取得します。  */
    @GetMapping("/loginAccount")
    fun loadLoginAccount(): LoginAccount {
        if (securityProps.auth.enabled) {
            val actorDetails = SecurityActorFinder.actorDetails()
                    .orElseThrow({ ValidationException(ErrorKeys.Authentication) })
            val actor = actorDetails.actor
            return LoginAccount(actor.id, actor.name, actorDetails.authorityIds)
        } else { // for dummy login
            return LoginAccount("sample", "sample", listOf())
        }
    }

}

/** クライアント利用用途に絞ったパラメタ  */
data class LoginAccount(
        val id: String,
        val name: String,
        val authorities: List<String>
)
