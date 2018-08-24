package sample.model.master

import sample.context.orm.OrmActiveRecord
import sample.context.orm.OrmRepository
import sample.model.constraints.Category
import sample.model.constraints.Currency
import sample.model.constraints.IdStr
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id


/**
 * サービス事業者の決済金融機関を表現します。
 * low: サンプルなので支店や名称、名義といったなど本来必須な情報をかなり省略しています。(通常は全銀仕様を踏襲します)
 */
@Entity
class SelfFiAccount(
        /** ID  */
        @Id
        @GeneratedValue
        var id: Long? = null,
        /** 利用用途カテゴリ  */
        @Category
        val category: String,
        /** 通貨  */
        @Currency
        val currency: String,
        /** 金融機関コード  */
        @IdStr
        val fiCode: String,
        /** 金融機関口座ID  */
        @IdStr
        val fiAccountId: String
) : OrmActiveRecord<SelfFiAccount>() {

    companion object {
        private const val serialVersionUID = 1L

        fun load(rep: OrmRepository, category: String, currency: String): SelfFiAccount {
            return rep.tmpl().load("FROM SelfFiAccount a WHERE a.category=?1 AND a.currency=?2", category, currency)
        }
    }

}
