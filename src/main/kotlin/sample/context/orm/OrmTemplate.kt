package sample.context.orm

import org.springframework.util.Assert
import sample.ErrorKeys
import sample.ValidationException
import java.io.Serializable
import java.util.*
import javax.persistence.EntityManager
import javax.persistence.Query
import javax.persistence.StoredProcedureQuery
import javax.persistence.TypedQuery
import javax.persistence.criteria.CriteriaQuery


/**
 * JPA の EntityManager に対する簡易アクセサ。 ( セッション毎に生成して利用してください )
 * <p>EntityManager のメソッドで利用したい処理があれば必要に応じてラップメソッドを追加してください。
 */
class OrmTemplate(
        val em: EntityManager,
        val metadata: Optional<OrmQueryMetadata> = Optional.empty()) {

    private fun <T> query(query: CriteriaQuery<T>): TypedQuery<T> {
        val q = em.createQuery(query)
        metadata.ifPresent { meta ->
            meta.hints.forEach({ k, v -> q.setHint(k, v) })
            meta.lockMode.ifPresent { q.setLockMode(it) }
        }
        return q
    }

    /** 指定したエンティティの ID 値を取得します。  */
    fun <T> idValue(entity: T): Serializable? {
        val clazz: Class<T> = (entity as Any).javaClass as Class<T>
        return OrmUtils.entityInformation(em, clazz).getId(entity)
    }

    /** Criteriaで一件取得します。  */
    fun <T> getByCriteria(criteria: CriteriaQuery<T>): Optional<T> =
        findByCriteria(criteria).stream().findFirst()

    /** Criteriaで一件取得します。(存在しない時はValidationException)  */
    fun <T> loadByCriteria(criteria: CriteriaQuery<T>): T =
        getByCriteria(criteria).orElseThrow { ValidationException(ErrorKeys.EntityNotFound) }

    /**
     * Criteria で検索します。
     * ※ランダムな条件検索等、可変条件検索が必要となる時に利用して下さい
     */
    fun <T> findByCriteria(criteria: CriteriaQuery<T>): List<T> =
        query(criteria).getResultList()

    /**
     * Criteria でページング検索します。
     *
     * Pagination に設定された検索条件は無視されます。 CriteriaQuery 構築時に設定するようにしてください。
     */
    fun <T> findByCriteria(criteria: CriteriaQuery<T>, criteriaCount: Optional<CriteriaQuery<Long>>, page: Pagination): PagingList<T> {
        Assert.notNull(page, "page is required")
        val total = criteriaCount.map { query(it).getResultList().get(0) }.orElse(-1L)
        if (total == 0L) return PagingList(mutableListOf(), Pagination.of(page, 0))

        val query = query<T>(criteria)
        if (0 < page.page) query.firstResult = page.firstResult
        if (0 < page.size) query.maxResults = page.size
        return PagingList<T>(query.resultList, Pagination.of(page, total))
    }


    /**
     * Criteriaで一件取得します。
     *
     * クロージャ戻り値は引数に取るOrmCriteriaのresult*の実行結果を返すようにしてください。
     */
    fun <T> getByCriteria(entityClass: Class<T>, func: (OrmCriteria<T>) -> CriteriaQuery<T>): Optional<T> =
        getByCriteria(func(OrmCriteria.of(em, entityClass)))

    fun <T> getByCriteria(entityClass: Class<T>, alias: String, func: (OrmCriteria<T>) -> CriteriaQuery<T>): Optional<T> =
        getByCriteria(func(OrmCriteria.of(em, entityClass, alias)))

    /**
     * Criteriaで一件取得します。(存在しない時はValidationException)
     *
     * クロージャ戻り値は引数に取るOrmCriteriaのresult*の実行結果を返すようにしてください。
     */
    fun <T> loadByCriteria(entityClass: Class<T>, func: (OrmCriteria<T>) -> CriteriaQuery<T>): T =
        loadByCriteria(func(OrmCriteria.of(em, entityClass)))

    fun <T> loadByCriteria(entityClass: Class<T>, alias: String, func: (OrmCriteria<T>) -> CriteriaQuery<T>): T =
        loadByCriteria(func(OrmCriteria.of(em, entityClass, alias)))

    /**
     * Criteriaで検索します。
     *
     * クロージャ戻り値は引数に取るOrmCriteriaのresult*の実行結果を返すようにしてください。
     */
    fun <T> findByCriteria(entityClass: Class<T>, func: (OrmCriteria<T>) -> CriteriaQuery<T>): List<T> =
        findByCriteria(func(OrmCriteria.of(em, entityClass)))

    fun <T> findByCriteria(entityClass: Class<T>, alias: String, func: (OrmCriteria<T>) -> CriteriaQuery<T>): List<T> =
        findByCriteria(func(OrmCriteria.of(em, entityClass, alias)))

    /**
     * Criteriaでページング検索します。
     *
     * Pagination に設定された検索条件は無視されます。 OrmCriteria 構築時に設定するようにしてください。
     */
    fun <T> findByCriteria(entityClass: Class<T>, func: (OrmCriteria<T>) -> OrmCriteria<T>, page: Pagination): PagingList<T> {
        val criteria = OrmCriteria.of(em, entityClass)
        func(criteria)
        return findByCriteria(criteria.result(), if (page.ignoreTotal) Optional.empty() else Optional.of(criteria.resultCount()), page)
    }

    fun <T> findByCriteria(entityClass: Class<T>, alias: String, func: (OrmCriteria<T>) -> OrmCriteria<T>, page: Pagination): PagingList<T> {
        val criteria = OrmCriteria.of(em, entityClass, alias)
        func(criteria)
        return findByCriteria(criteria.result(), if (page.ignoreTotal) Optional.empty() else Optional.of(criteria.resultCount()), page)
    }

    /**
     * Criteriaでページング検索します。
     *
     * CriteriaQuery が提供する subquery や groupBy 等の構文を利用したい時はこちらの extension で指定してください。
     *
     * Pagination に設定された検索条件は無視されます。 OrmCriteria 構築時に設定するようにしてください。
     */
    fun <T> findByCriteria(entityClass: Class<T>, func: (OrmCriteria<T>) -> OrmCriteria<T>,
                           extension: (CriteriaQuery<*>) -> CriteriaQuery<*>, page: Pagination): PagingList<T> {
        val criteria = OrmCriteria.of(em, entityClass)
        func(criteria)
        return findByCriteria(criteria.result(extension), if (page.ignoreTotal) Optional.empty() else Optional.of(criteria.resultCount(extension)), page)
    }

    fun <T> findByCriteria(entityClass: Class<T>, alias: String, func: (OrmCriteria<T>) -> OrmCriteria<T>,
                           extension: (CriteriaQuery<*>) -> CriteriaQuery<*>, page: Pagination): PagingList<T> {
        val criteria = OrmCriteria.of(em, entityClass, alias)
        func(criteria)
        return findByCriteria(criteria.result(extension), if (page.ignoreTotal) Optional.empty() else Optional.of(criteria.resultCount(extension)), page)
    }

    /**
     * JPQL で一件取得します。
     *
     * args に Map を指定した時は名前付き引数として取り扱います。 ( Map のキーには文字列を指定してください )
     */
    fun <T> get(qlString: String, vararg args: Any): Optional<T> {
        val list = find<T>(qlString, *args)
        return list.stream().findFirst()
    }

    /**
     * JPQL で一件取得します。(存在しない時は ValidationException )
     *
     * args に Map を指定した時は名前付き引数として取り扱います。 ( Map のキーには文字列を指定してください )
     */
    fun <T> load(qlString: String, vararg args: Any): T {
        val v = get<T>(qlString, *args)
        return v.orElseThrow { ValidationException(ErrorKeys.EntityNotFound) }
    }

    /** 対象 Entity を全件取得します。 */
    fun <T> loadAll(entityClass: Class<T>): List<T> =
        findByCriteria(OrmCriteria.of<T>(em, entityClass).result())

    /**
     * JPQL で検索します。
     *
     * args に Map を指定した時は名前付き引数として取り扱います。 ( Map のキーには文字列を指定してください )
     */
    fun <T> find(qlString: String, vararg args: Any): List<T> =
        bindArgs(em.createQuery(qlString), *args).getResultList() as List<T>

    /**
     * JPQL でページング検索します。
     *
     * カウント句がうまく構築されない時はPagination#ignoreTotalをtrueにして、
     * 別途通常の検索でトータル件数を算出するようにして下さい。
     *
     * page に設定されたソート条件は無視されるので、 qlString 構築時に明示的な設定をしてください。
     *
     * args に Map を指定した時は名前付き引数として取り扱います。 ( Map のキーには文字列を指定してください )
     */
    fun <T> find(qlString: String, page: Pagination, vararg args: Any): PagingList<T> {
        val total = if (page.ignoreTotal) -1L else load(OrmUtils.createCountQueryFor(qlString), args)
        val list = bindArgsWithPage(em.createQuery(qlString), page, *args).getResultList() as List<T>
        return PagingList(list, Pagination.of(page, total))
    }

    /**
     * 定義済み JPQL で一件取得します。
     *
     * 事前に name に合致する @NamedQuery 定義が必要です。
     *
     * args に Map を指定した時は名前付き引数として取り扱います。 ( Map のキーには文字列を指定してください )
     */
    fun <T> getNamed(name: String, vararg args: Any): Optional<T> {
        val list = findNamed<T>(name, *args)
        return list.stream().findFirst()
    }

    /**
     * 定義済み JPQL で一件取得をします。(存在しない時は ValidationException )
     *
     * 事前に name に合致する @NamedQuery 定義が必要です。
     *
     * args に Map を指定した時は名前付き引数として取り扱います。 ( Map のキーには文字列を指定してください )
     */
    fun <T> loadNamed(name: String, vararg args: Any): T {
        val v = getNamed<T>(name, *args)
        return v.orElseThrow { ValidationException(ErrorKeys.EntityNotFound) }
    }

    /**
     * 定義済み JPQL で検索します。
     *
     * 事前に name に合致する @NamedQuery 定義が必要です。
     *
     * args に Map を指定した時は名前付き引数として取り扱います。 ( Map のキーには文字列を指定してください )
     */
    fun <T> findNamed(name: String, vararg args: Any): List<T> =
        bindArgs(em.createNamedQuery(name), *args).resultList as List<T>

    /**
     * 定義済み JPQL でページング検索します。
     *
     * 事前に name に合致する @NamedQuery 定義が必要です。
     *
     * args に Map を指定した時は名前付き引数として取り扱います。 ( Map のキーには文字列を指定してください )
     *
     * page に設定されたソート条件は無視されます。
     */
    fun <T> findNamed(name: String, nameCount: String, page: Pagination, args: Map<String, Any>): PagingList<T> {
        val total = if (page.ignoreTotal) -1L else loadNamed(nameCount, args)
        val list = bindArgsWithPage(em.createNamedQuery(name), page, args).getResultList() as List<T>
        return PagingList(list, Pagination.of(page, total))
    }

    /**
     * SQLで検索します。
     *
     * 検索結果としてselectの値配列一覧が返されます。
     *
     * args に Map を指定した時は名前付き引数として取り扱います。 ( Map のキーには文字列を指定してください )
     */
    fun <T> findBySql(sql: String, vararg args: Any): List<T> =
        bindArgs(em.createNativeQuery(sql), *args).getResultList() as List<T>

    /**
     * SQL で検索します。
     *
     * args に Map を指定した時は名前付き引数として取り扱います。 ( Map のキーには文字列を指定してください )
     */
    fun <T> findBySql(sql: String, clazz: Class<T>, vararg args: Any): List<T> =
        bindArgs(em.createNativeQuery(sql, clazz), *args).getResultList() as List<T>

    /**
     * SQL でページング検索します。
     *
     * 検索結果として select の値配列一覧が返されます。
     *
     * args に Map を指定した時は名前付き引数として取り扱います。 ( Map のキーには文字列を指定してください )
     */
    fun <T> findBySql(sql: String, sqlCount: String, page: Pagination, vararg args: Any): PagingList<T> {
        val total = if (page.ignoreTotal) -1L else findBySql<Any>(sqlCount, *args).stream().findFirst().map { v -> java.lang.Long.parseLong(v.toString()) }.orElse(0L)
        val list = bindArgsWithPage(em.createNativeQuery(sql), page, *args).getResultList() as List<T>
        return PagingList<T>(list, Pagination.of(page, total))
    }

    /**
     * SQL でページング検索します。
     *
     * page に設定されたソート条件は無視されるので、 sql 構築時に明示的な設定をしてください。
     *
     * args に Map を指定した時は名前付き引数として取り扱います。 ( Map のキーには文字列を指定してください )
     */
    fun <T> findBySql(sql: String, sqlCount: String, clazz: Class<T>, page: Pagination, vararg args: Any): PagingList<T> {
        val total = if (page.ignoreTotal) -1L else findBySql<Any>(sqlCount, *args).stream().findFirst().map { v -> java.lang.Long.parseLong(v.toString()) }.orElse(0L)
        val list = bindArgsWithPage(em.createNativeQuery(sql, clazz), page, *args).getResultList() as List<T>
        return PagingList<T>(list, Pagination.of(page, total))
    }

    /**
     * JPQL を実行します。
     *
     * args に Map を指定した時は名前付き引数として取り扱います。 ( Map のキーには文字列を指定してください )
     */
    fun execute(qlString: String, vararg args: Any): Int =
        bindArgs(em.createQuery(qlString), *args).executeUpdate()

    /**
     * 定義済み JPQL を実行します。
     *
     * 事前に name に合致する @NamedQuery 定義が必要です。
     *
     * args に Map を指定した時は名前付き引数として取り扱います。 ( Map のキーには文字列を指定してください )
     */
    fun executeNamed(name: String, vararg args: Any): Int =
        bindArgs(em.createNamedQuery(name), *args).executeUpdate()

    /**
     * SQL を実行をします。
     *
     * args に Map を指定した時は名前付き引数として取り扱います。 ( Map のキーには文字列を指定してください )
     */
    fun executeSql(sql: String, vararg args: Any): Int =
        bindArgs(em.createNativeQuery(sql), *args).executeUpdate()

    /** ストアド を処理をします。 */
    fun callStoredProcedure(procedureName: String, proc: (StoredProcedureQuery) -> Unit) =
        proc(bindArgs(em.createStoredProcedureQuery(procedureName)) as StoredProcedureQuery)

    /**
     * クエリに値を紐付けします。
     *
     * Map 指定時はキーに文字を指定します。それ以外は自動的に 1 開始のポジション指定をおこないます。
     */
    fun bindArgs(query: Query, vararg args: Any): Query =
        bindArgsWithPage(query = query, page =null, args = *args)

    fun bindArgsWithPage(query: Query, page: Pagination?, vararg args: Any): Query {
        if (page != null) {
            if (page.page > 0)
                query.setFirstResult(page.firstResult)
            if (page.size > 0)
                query.setMaxResults(page.size)
        }
        for (i in args.indices) {
            val arg = args[i]
            if (arg is Map<*, *>) {
                val argNamed = arg as Map<String, Any>
                argNamed.forEach { k, v -> query.setParameter(k, v) }
            } else {
                query.setParameter(i + 1, args[i])
            }
        }
        return query
    }

}
