package sample.model.master

import org.hibernate.criterion.MatchMode
import org.springframework.security.crypto.password.PasswordEncoder
import sample.ErrorKeys
import sample.context.Dto
import sample.context.actor.Actor
import sample.context.actor.ActorRoleType
import sample.context.orm.JpqlBuilder
import sample.context.orm.OrmActiveRecord
import sample.context.orm.OrmRepository
import sample.model.constraints.IdStr
import sample.model.constraints.Name
import sample.model.constraints.OutlineEmpty
import sample.model.constraints.Password
import sample.util.Validator
import java.util.*
import javax.persistence.Entity
import javax.persistence.Id


/**
 * 社員を表現します。
 */
@Entity
data class Staff(
        /** ID  */
        @Id
        @field:IdStr
        var id: String,
        /** 名前  */
        @field:Name
        var name: String,
        /** パスワード(暗号化済)  */
        @field:Password
        var password: String
) : OrmActiveRecord<Staff>() {

    fun actor(): Actor {
        return Actor(id = id, name = name, roleType = ActorRoleType.Internal)
    }

    /** パスワードを変更します。  */
    fun change(rep: OrmRepository, encoder: PasswordEncoder, p: ChgPassword): Staff =
        p.bind(this, encoder.encode(p.plainPassword)).update(rep)

    /** 社員情報を変更します。  */
    fun change(rep: OrmRepository, p: ChgStaff): Staff =
        p.bind(this).update(rep)

    companion object {
        private const val serialVersionUID = 1L

        /** 社員を取得します。  */
        fun get(rep: OrmRepository, id: String): Optional<Staff> =
            rep.get(Staff::class.java, id)

        /** 社員を取得します。(例外付)  */
        fun load(rep: OrmRepository, id: String): Staff =
            rep.load(Staff::class.java, id)

        /** 社員を検索します。  */
        fun find(rep: OrmRepository, p: FindStaff): List<Staff> {
            val jpql = JpqlBuilder.of("FROM Staff s")
                    .like(listOf("id", "name"), p.keyword, MatchMode.ANYWHERE)
            return rep.tmpl().find(jpql.build(), *jpql.args())
        }

        /** 社員の登録を行います。  */
        fun register(rep: OrmRepository, encoder: PasswordEncoder, p: RegStaff): Staff {
            Validator.validate {
                it.checkField(!get(rep, p.id!!).isPresent, "id", ErrorKeys.DuplicateId)
            }
            return p.create(encoder.encode(p.plainPassword)).save(rep)
        }
    }

}

/** 検索パラメタ  */
data class FindStaff(
        @field:OutlineEmpty
        val keyword: String? = null
) : Dto

/** 登録パラメタ  */
data class RegStaff(
        @field:IdStr
        val id: String? = null,
        @field:Name
        val name: String? = null,
        /** パスワード(未ハッシュ)  */
        @field:Password
        val plainPassword: String? = null
) : Dto {
    fun create(password: String): Staff =
            Staff(id = id!!, name = name!!, password = password)
}

/** 変更パラメタ  */
data class ChgStaff(
        @field:Name
        val name: String? = null
) : Dto {
    fun bind(m: Staff): Staff {
        m.name = name!!
        return m
    }
}

/** パスワード変更パラメタ  */
data class ChgPassword(
        @field:Password
        val plainPassword: String? = null
) : Dto {
    fun bind(m: Staff, password: String): Staff {
        m.password = password
        return m
    }
}
