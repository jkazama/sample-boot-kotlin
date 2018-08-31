package sample.context.orm

import org.springframework.beans.factory.ObjectProvider
import sample.ErrorKeys
import sample.ValidationException
import sample.context.DomainHelper
import sample.context.Entity
import sample.context.Repository
import java.io.Serializable
import java.util.*
import javax.persistence.EntityManager
import javax.persistence.EntityNotFoundException
import javax.persistence.LockModeType

/**
 * JPA ( Hibernate ) の Repository 基底実装。
 * <p>本コンポーネントは Repository と Entity の 1-n 関係を実現するために SpringData の基盤を
 * 利用しない形で単純な ORM 実装を提供します。
 * <p>OrmRepository を継承して作成される Repository の粒度はデータソース単位となります。
 */
abstract class OrmRepository(
        private val dh: ObjectProvider<DomainHelper>,
        private val interceptor: ObjectProvider<OrmInterceptor>
) : Repository {

    /**
     * 管理するEntityManagerを返します。
     * 継承先で管理したいデータソースのEntityManagerを返してください。
     */
    abstract fun em(): EntityManager

    /** {@inheritDoc}  */
    override fun dh(): DomainHelper = dh.getObject()

    fun interceptor(): Optional<OrmInterceptor> {
        return Optional.ofNullable(interceptor.ifAvailable)
    }

    /**
     * ORM操作の簡易アクセサを生成します。
     *
     * OrmTemplateは呼出しの都度生成されます。
     */
    fun tmpl(): OrmTemplate {
        return OrmTemplate(em())
    }

    fun tmpl(metadata: OrmQueryMetadata): OrmTemplate {
        return OrmTemplate(em(), Optional.of(metadata))
    }

    /** 指定したEntityクラスを軸にしたCriteriaを生成します。  */
    fun <T : Entity> criteria(clazz: Class<T>): OrmCriteria<T> {
        return OrmCriteria.of(em(), clazz)
    }

    /** 指定したEntityクラスにエイリアスを紐付けたCriteriaを生成します。  */
    fun <T : Entity> criteria(clazz: Class<T>, alias: String): OrmCriteria<T> {
        return OrmCriteria.of(em(), clazz, alias)
    }

    /** {@inheritDoc}  */
    override fun <T : Entity> get(clazz: Class<T>, id: Serializable): Optional<T> {
        val m = em().find(clazz, id)
        m?.hashCode() // force loading
        return Optional.ofNullable(m)
    }

    /** {@inheritDoc}  */
    override fun <T : Entity> load(clazz: Class<T>, id: Serializable): T {
        try {
            val m = em().getReference(clazz, id)
            m.hashCode() // force loading
            return m
        } catch (e: EntityNotFoundException) {
            throw ValidationException(ErrorKeys.EntityNotFound)
        }

    }

    /** {@inheritDoc}  */
    override fun <T : Entity> loadForUpdate(clazz: Class<T>, id: Serializable): T {
        val m = em().find(clazz, id, LockModeType.PESSIMISTIC_WRITE)
                ?: throw ValidationException(ErrorKeys.EntityNotFound)
        m.hashCode() // force loading
        return m
    }

    /** {@inheritDoc}  */
    override fun <T : Entity> exists(clazz: Class<T>, id: Serializable): Boolean {
        return get(clazz, id).isPresent
    }

    /** {@inheritDoc}  */
    override fun <T : Entity> findAll(clazz: Class<T>): List<T> {
        return tmpl().loadAll(clazz)
    }

    /** {@inheritDoc}  */
    override fun <T : Entity> save(entity: T): T {
        interceptor().ifPresent { it.touchForCreate(entity)}
        em().persist(entity)
        return entity
    }

    /** {@inheritDoc}  */
    override fun <T : Entity> saveOrUpdate(entity: T): T {
        interceptor().ifPresent { it.touchForUpdate(entity) }
        return em().merge(entity)
    }

    /** {@inheritDoc}  */
    override fun <T : Entity> update(entity: T): T {
        interceptor().ifPresent { it.touchForUpdate(entity) }
        return em().merge(entity)
    }

    /** {@inheritDoc}  */
    override fun <T : Entity> delete(entity: T): T {
        em().remove(entity)
        return entity
    }

    /**
     * セッションキャッシュ中の永続化されていないエンティティを全てDBと同期(SQL発行)します。
     *
     * SQL発行タイミングを明確にしたい箇所で呼び出すようにしてください。バッチ処理などでセッションキャッシュが
     * メモリを逼迫するケースでは#flushAndClearを定期的に呼び出してセッションキャッシュの肥大化を防ぐようにしてください。
     */
    fun flush(): OrmRepository {
        em().flush()
        return this
    }

    /**
     * セッションキャッシュ中の永続化されていないエンティティをDBと同期化した上でセッションキャッシュを初期化します。
     *
     * 大量の更新が発生するバッチ処理などでは暗黙的に保持されるセッションキャッシュがメモリを逼迫して
     * 大きな問題を引き起こすケースが多々見られます。定期的に本処理を呼び出してセッションキャッシュの
     * サイズを定量に維持するようにしてください。
     */
    fun flushAndClear(): OrmRepository {
        em().flush()
        em().clear()
        return this
    }


}
