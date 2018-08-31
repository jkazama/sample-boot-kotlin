package sample.context.orm

import sample.context.Entity
import sample.util.Validator
import java.io.Serializable


/**
 * ORMベースでActiveRecordの概念を提供するEntity基底クラス。
 * <p>ここでは自インスタンスの状態に依存する簡易な振る舞いのみをサポートします。
 * 実際のActiveRecordモデルにはget/find等の概念も含まれますが、それらは 自己の状態を
 * 変える行為ではなく対象インスタンスを特定する行為(クラス概念)にあたるため、
 * クラスメソッドとして継承先で個別定義するようにしてください。
 * <pre>
 * public static Optional&lt;Account&gt; get(final OrmRepository rep, String id) {
 *     return rep.get(Account.class, id);
 * }
 *
 * public static Account findAll(final OrmRepository rep) {
 *     return rep.findAll(Account.class);
 * }
 * </pre>
 */
@Suppress("UNCHECKED_CAST")
abstract class OrmActiveRecord<T : Entity> : Serializable, Entity {

    /** 審査処理をします。  */
    protected fun validate(proc: (Validator) -> Unit): T {
        Validator.validate(proc)
        return this as T
    }

    /**
     * 与えられたレポジトリを経由して自身を新規追加します。
     * @param rep 永続化の際に利用する関連[OrmRepository]
     * @return 自身の情報
     */
    fun save(rep: OrmRepository): T =
            rep.save(this as T)

    /**
     * 与えられたレポジトリを経由して自身を更新します。
     * @param rep 永続化の際に利用する関連[OrmRepository]
     */
    fun update(rep: OrmRepository): T =
            rep.update(this as T)

    /**
     * 与えられたレポジトリを経由して自身を物理削除します。
     * @param rep 永続化の際に利用する関連[OrmRepository]
     */
    fun delete(rep: OrmRepository): T =
            rep.delete(this as T)

    /**
     * 与えられたレポジトリを経由して自身を新規追加または更新します。
     * @param rep 永続化の際に利用する関連[OrmRepository]
     */
    fun saveOrUpdate(rep: OrmRepository): T =
            rep.saveOrUpdate(this as T)

}