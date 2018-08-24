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
class Staff(
        /** ID  */
        @Id
        @IdStr
        var id: String? = null,
        /** 名前  */
        @Name
        var name: String,
        /** パスワード(暗号化済)  */
        @Password
        var password: String
) : OrmActiveRecord<Staff>() {

    fun actor(): Actor {
        return Actor(id = id!!, name = name, roleType = ActorRoleType.Internal)
    }

    /** パスワードを変更します。  */
    fun change(rep: OrmRepository, encoder: PasswordEncoder, p: ChgPassword): Staff {
        return p.bind(this, encoder.encode(p.plainPassword)).update(rep)
    }

    /** 社員情報を変更します。  */
    fun change(rep: OrmRepository, p: ChgStaff): Staff {
        return p.bind(this).update(rep)
    }

    companion object {
        private const val serialVersionUID = 1L

        /** 社員を取得します。  */
        fun get(rep: OrmRepository, id: String): Optional<Staff> {
            return rep.get(Staff::class.java, id)
        }

        /** 社員を取得します。(例外付)  */
        fun load(rep: OrmRepository, id: String): Staff {
            return rep.load(Staff::class.java, id)
        }

        /** 社員を検索します。  */
        fun find(rep: OrmRepository, p: FindStaff): List<Staff> {
            val jpql = JpqlBuilder.of("FROM Staff s")
                    .like(listOf("id", "name"), p.keyword, MatchMode.ANYWHERE)
            return rep.tmpl().find(jpql.build(), *jpql.args())
        }

        /** 社員の登録を行います。  */
        fun register(rep: OrmRepository, encoder: PasswordEncoder, p: RegStaff): Staff {
            Validator.validate({ it.checkField(!get(rep, p.id).isPresent(), "id", ErrorKeys.DuplicateId) })
            return p.create(encoder.encode(p.plainPassword)).save(rep)
        }
    }

}

/** 検索パラメタ  */
data class FindStaff(
        @OutlineEmpty
        val keyword: String? = null
) : Dto

/** 登録パラメタ  */
data class RegStaff(
        @IdStr
        val id: String,
        @Name
        val name: String,
        /** パスワード(未ハッシュ)  */
        @Password
        val plainPassword: String
) : Dto {
    fun create(password: String): Staff =
            Staff(id = id, name = name, password = password)
}

/** 変更パラメタ  */
data class ChgStaff(
        @Name
        val name: String
) : Dto {
    fun bind(m: Staff): Staff {
        m.name = name
        return m
    }
}

/** パスワード変更パラメタ  */
data class ChgPassword(
        @Password
        val plainPassword: String
) : Dto {
    fun bind(m: Staff, password: String): Staff {
        m.password = password
        return m
    }
}
