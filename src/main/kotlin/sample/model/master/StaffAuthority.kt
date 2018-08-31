package sample.model.master

import sample.context.orm.OrmActiveRecord
import sample.context.orm.OrmRepository
import sample.model.constraints.IdStr
import sample.model.constraints.Name
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id


/**
 * 社員に割り当てられた権限を表現します。
 */
@Entity
data class StaffAuthority(
        /** ID  */
        @Id
        @GeneratedValue
        var id: Long? = null,
        /** 社員ID  */
        @field:IdStr
        val staffId: String,
        /** 権限名称。(「プリフィックスにROLE_」を付与してください)  */
        @field:Name
        val authority: String
) : OrmActiveRecord<StaffAuthority>() {
    companion object {
        private const val serialVersionUID = 1L

        /** 口座IDに紐付く権限一覧を返します。  */
        fun find(rep: OrmRepository, staffId: String): List<StaffAuthority> =
            rep.tmpl().find("FROM StaffAuthority WHERE staffId=?1", staffId)
    }

}
