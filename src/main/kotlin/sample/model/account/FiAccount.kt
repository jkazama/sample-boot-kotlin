package sample.model.account

import sample.context.orm.OrmActiveRecord
import sample.context.orm.OrmRepository
import sample.model.constraints.Category
import sample.model.constraints.Currency
import sample.model.constraints.IdStr
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id


/**
 * 口座に紐づく金融機関口座を表現します。
 *
 * 口座を相手方とする入出金で利用します。
 * low: サンプルなので支店や名称、名義といった本来必須な情報をかなり省略しています。(通常は全銀仕様を踏襲します)
 */
@Entity
class FiAccount(
        /** ID  */
        @Id
        @GeneratedValue
        var id: Long? = null,
        /** 口座ID  */
        @IdStr
        val accountId: String,
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
) : OrmActiveRecord<FiAccount>() {
    companion object {
        private const val serialVersionUID = 1L

        fun load(rep: OrmRepository, accountId: String, category: String, currency: String): FiAccount =
                rep.tmpl().load("FROM FiAccount a WHERE a.accountId=?1 AND a.category=?2 AND a.currency=?3",
                        accountId, category, currency)
    }
}
