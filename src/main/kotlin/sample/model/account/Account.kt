package sample.model.account

import org.springframework.security.crypto.password.PasswordEncoder
import sample.ErrorKeys
import sample.ValidationException
import sample.context.Dto
import sample.context.actor.Actor
import sample.context.actor.ActorRoleType
import sample.context.orm.OrmActiveRecord
import sample.context.orm.OrmRepository
import sample.model.constraints.Email
import sample.model.constraints.IdStr
import sample.model.constraints.Name
import sample.model.constraints.Password
import sample.util.Validator
import java.util.*
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.Id
import javax.validation.constraints.NotNull


/**
 * 口座を表現します。
 * low: サンプル用に必要最低限の項目だけ
 */
@Entity
class Account(
        /** 口座ID  */
        @Id
        @IdStr
        var id: String? = null,
        /** 口座名義  */
        @Name
        var name: String,
        /** メールアドレス  */
        @Email
        var mail: String,
        /** 口座状態  */
        @NotNull
        @Enumerated(EnumType.STRING)
        var statusType: AccountStatusType
) : OrmActiveRecord<Account>() {

    fun actor(): Actor =
            Actor(id = id!!, roleType = ActorRoleType.User, name = name)

    /** 口座に紐付くログイン情報を取得します。  */
    fun loadLogin(rep: OrmRepository): Login =
            Login.load(rep, id!!)

    /** 口座を変更します。  */
    fun change(rep: OrmRepository, p: ChgAccount): Account =
            p.bind(this).update(rep)

    companion object {
        private const val serialVersionUID = 1L

        /** 口座を取得します。  */
        fun get(rep: OrmRepository, id: String?): Optional<Account> =
                rep.get(Account::class.java, id!!)

        /** 有効な口座を取得します。  */
        fun getValid(rep: OrmRepository, id: String): Optional<Account> =
                get(rep, id).filter({ acc -> acc.statusType.valid() })

        /** 口座を取得します。(例外付)  */
        fun load(rep: OrmRepository, id: String): Account =
                rep.load(Account::class.java, id)

        /** 有効な口座を取得します。(例外付)  */
        fun loadValid(rep: OrmRepository, id: String): Account =
                getValid(rep, id).orElseThrow({ ValidationException("error.Account.loadValid") })

        /**
         * 口座の登録を行います。
         *
         * ログイン情報も同時に登録されます。
         */
        fun register(rep: OrmRepository, encoder: PasswordEncoder, p: RegAccount): Account {
            Validator.validate({ v -> v.checkField(!get(rep, p.id).isPresent(), "id", ErrorKeys.DuplicateId) })
            p.createLogin(encoder.encode(p.plainPassword)).save(rep)
            return p.create().save(rep)
        }
    }

}

enum class AccountStatusType {
    /** 通常 */
    Normal,
    /** 退会 */
    Withdrawal;

    fun valid(): Boolean = this == Normal
    fun invalid(): Boolean = !valid()
}

/** 登録パラメタ  */
data class RegAccount(
        @IdStr
        val id: String,
        @Name
        val name: String,
        @Email
        val mail: String,
        /** パスワード(未ハッシュ)  */
        @Password
        val plainPassword: String
) : Dto {
    fun create(): Account =
            Account(
                    id = id,
                    name = name,
                    mail = mail,
                    statusType = AccountStatusType.Normal
            )

    fun createLogin(password: String): Login =
            Login(
                    id = id,
                    loginId = id,
                    password = password
            )
}

/** 変更パラメタ  */
data class ChgAccount(
        @Name
        val name: String,
        @Email
        val mail: String
) : Dto {
    fun bind(m: Account): Account {
        m.name = name
        m.mail = mail
        return m
    }
}