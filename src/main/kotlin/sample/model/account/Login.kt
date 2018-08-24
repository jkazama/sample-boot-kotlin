package sample.model.account

import org.springframework.security.crypto.password.PasswordEncoder
import sample.ErrorKeys
import sample.context.Dto
import sample.context.orm.OrmActiveRecord
import sample.context.orm.OrmRepository
import sample.model.constraints.IdStr
import sample.model.constraints.Password
import java.util.*
import javax.persistence.Entity
import javax.persistence.Id

/**
 * 口座ログインを表現します。
 * low: サンプル用に必要最低限の項目だけ
 */
@Entity
class Login(
        /** 口座ID  */
        @Id
        @IdStr
        var id: String? = null,
        /** ログインID  */
        @IdStr
        var loginId: String,
        /** パスワード(暗号化済)  */
        @Password
        var password: String
) : OrmActiveRecord<Login>() {

    /** ログインIDを変更します。  */
    fun change(rep: OrmRepository, p: ChgLoginId): Login {
        val exists = rep.tmpl().get<Login>("FROM Login l WHERE l.id<>?1 AND l.loginId=?2", id!!, p.loginId).isPresent
        validate { v -> v.checkField(!exists, "loginId", ErrorKeys.DuplicateId) }
        return p.bind(this).update(rep)
    }

    /** パスワードを変更します。  */
    fun change(rep: OrmRepository, encoder: PasswordEncoder, p: ChgPassword): Login =
            p.bind(this, encoder.encode(p.plainPassword)).update(rep)

    companion object {
        private const val serialVersionUID = 1L

        /** ログイン情報を取得します。  */
        fun get(rep: OrmRepository, id: String): Optional<Login> =
                rep.get(Login::class.java, id)

        /** ログイン情報を取得します。  */
        fun getByLoginId(rep: OrmRepository, loginId: String): Optional<Login> =
                Optional.ofNullable(loginId).flatMap({ rep.tmpl().get<Login>("FROM Login l WHERE loginId=?1", it) })

        /** ログイン情報を取得します。(例外付)  */
        fun load(rep: OrmRepository, id: String): Login =
                rep.load(Login::class.java, id)
    }

}

/** ログインID変更パラメタ low: 基本はユースケース単位で切り出す  */
data class ChgLoginId(
        @IdStr
        val loginId: String
) : Dto {
    fun bind(m: Login): Login {
        m.loginId = loginId
        return m
    }
}

/** パスワード変更パラメタ  */
data class ChgPassword(
        @Password
        val plainPassword: String
) : Dto {
    fun bind(m: Login, password: String): Login {
        m.password = password
        return m
    }
}
