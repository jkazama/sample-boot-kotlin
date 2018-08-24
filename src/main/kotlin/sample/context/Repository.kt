package sample.context

import java.io.Serializable
import java.util.*

/**
 * 特定のドメインオブジェクトに依存しない汎用的なRepositoryです。
 * <p>タイプセーフでないRepositoryとして利用することができます。
 */
interface Repository {

    /**
     * @return ドメイン層においてインフラ層コンポーネントへのアクセスを提供するヘルパーユーティリティを返します。
     */
    fun dh(): DomainHelper

    /**
     * プライマリキーに一致する[Entity]を返します。
     * @param <T> 戻り値の型
     * @param clazz 取得するインスタンスのクラス
     * @param id プライマリキー
     * @return プライマリキーに一致した{@link Entity}。
     */
    fun <T : Entity> get(clazz: Class<T>, id: Serializable): Optional<T>

    /**
     * プライマリキーに一致する[Entity]を返します。
     * @param <T> 戻り値の型
     * @param clazz 取得するインスタンスのクラス
     * @param id プライマリキー
     * @return プライマリキーに一致した{@link Entity}。一致しない時は例外。
     */
    fun <T : Entity> load(clazz: Class<T>, id: Serializable): T

    /**
     * プライマリキーに一致する[Entity]を返します。
     *
     * ロック付(for update)で取得を行うため、デッドロック回避を意識するようにしてください。
     * @param <T> 戻り値の型
     * @param clazz 取得するインスタンスのクラス
     * @param id プライマリキー
     * @return プライマリキーに一致した{@link Entity}。一致しない時は例外。
     */
    fun <T : Entity> loadForUpdate(clazz: Class<T>, id: Serializable): T

    /**
     * プライマリキーに一致する[Entity]が存在するか返します。
     * @param <T> 確認型
     * @param clazz 対象クラス
     * @param id プライマリキー
     * @return 存在する時はtrue
     */
    fun <T : Entity> exists(clazz: Class<T>, id: Serializable): Boolean

    /**
     * 管理する[Entity]を全件返します。
     * 条件検索などは#templateを利用して実行するようにしてください。
     * @param <T> 戻り値の型
     * @param clazz 取得するインスタンスのクラス
     * @return [Entity]一覧
     */
    fun <T : Entity> findAll(clazz: Class<T>): List<T>

    /**
     * [Entity]を新規追加します。
     * @param entity 追加対象[Entity]
     * @return 追加した{@link Entity}のプライマリキー
     */
    fun <T : Entity> save(entity: T): T

    /**
     * [Entity]を新規追加または更新します。
     *
     * 既に同一のプライマリキーが存在するときは更新。
     * 存在しない時は新規追加となります。
     * @param entity 追加対象[Entity]
     */
    fun <T : Entity> saveOrUpdate(entity: T): T

    /**
     * [Entity]を更新します。
     * @param entity 更新対象[Entity]
     */
    fun <T : Entity> update(entity: T): T

    /**
     * [Entity]を削除します。
     * @param entity 削除対象[Entity]
     */
    fun <T : Entity> delete(entity: T): T

}