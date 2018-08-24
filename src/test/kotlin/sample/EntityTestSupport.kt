package sample

import org.junit.After
import org.junit.Before
import org.springframework.orm.jpa.SharedEntityManagerCreator
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import sample.context.Entity
import sample.context.Timestamper
import sample.context.actor.ActorSession
import sample.context.orm.*
import sample.model.BusinessDayHandler
import sample.model.DataFixtures
import sample.support.MockDomainHelper
import java.time.Clock
import java.util.*
import javax.persistence.EntityManager
import javax.persistence.EntityManagerFactory
import javax.sql.DataSource


/**
 * Spring コンテナを用いない JPA のみに特化した検証用途。
 *
 * model パッケージでのみ利用してください。
 */
open class EntityTestSupport(
        var clock: Clock = Clock.systemDefaultZone(),
        var dh: MockDomainHelper = MockDomainHelper(mockClock = clock),
        var time: Timestamper = dh.time,
        var session: ActorSession = dh.actorSession,
        var businessDay: BusinessDayHandler = BusinessDayHandler(time = dh.time),
        var encoder: PasswordEncoder = BCryptPasswordEncoder()
) {
    private var emf: EntityManagerFactory? = null
    private var rep: DefaultRepository? = null
    private var repSystem: SystemRepository? = null
    private var txm: PlatformTransactionManager? = null
    private var fixtures: DataFixtures? = null

    /** テスト対象とするパッケージパス(通常はtargetEntitiesの定義を推奨)  */
    var packageToScan = "sample"
    /** テスト対象とするEntityクラス一覧  */
    var targetEntities: List<Class<*>> = ArrayList()

    fun rep() = rep!!
    fun repSystem() = repSystem!!
    fun txm() = txm!!
    fun fixtures() = fixtures!!

    @Before
    open fun setup() {
        setupPreset()
        setupRepository()
        setupDataFixtures()
        before()
    }

    /** 設定事前処理。repインスタンス生成前  */
    open fun setupPreset() {
        // 各Entity検証で上書きしてください
    }

    /** 事前処理。repインスタンス生成後  */
    open fun before() {
        // 各Entity検証で上書きしてください
    }

    /**
     * [.setupPreset]内で対象Entityを指定してください。
     * (targetEntitiesといずれかを設定する必要があります)
     */
    fun targetPackage(packageToScan: String) {
        this.packageToScan = packageToScan
    }

    /**
     * [.setupPreset]内で対象Entityを指定してください。
     * (targetPackageといずれかを設定する必要があります)
     */
    fun targetEntities(vararg list: Class<*>) {
        this.targetEntities = list.toList()
    }

    /**
     * [.setupPreset]内で利用したいClockを指定してください。
     */
    fun clock(clock: Clock) {
        this.clock = clock
    }

    /**
     * [.before]内でモック設定値を指定してください。
     */
    fun setting(id: String, value: String) {
        dh.setting(id, value)
    }

    @After
    fun cleanup() {
        emf!!.close()
    }

    fun setupRepository() {
        setupEntityManagerFactory()
        val em = SharedEntityManagerCreator.createSharedEntityManager(emf!!)
        rep = DefaultRepository(dh, entityInterceptor(), em)
        repSystem = SystemRepository(dh, entityInterceptor(), em)
    }

    fun setupDataFixtures() {
        fixtures = DataFixtures(
                time = time,
                businessDay = businessDay,
                encoder = encoder,
                rep = rep!!,
                tx = txm!!,
                repSystem = repSystem!!,
                txSystem = txm!!
        )
    }

    fun setupEntityManagerFactory() {
        val ds = EntityTestFactory.dataSource()
        val props = DefaultDataSourceProperties()
        props.jpa.setShowSql(true)
        props.jpa.getHibernate().setDdlAuto("create-drop")
        if (targetEntities.isEmpty()) {
            props.jpa.packageToScan = arrayOf(packageToScan)
        } else {
            props.jpa.annotatedClasses = targetEntities.toTypedArray()
        }

        val emfBean = props.entityManagerFactoryBean(ds)
        emfBean.afterPropertiesSet()
        emf = emfBean.getObject()
        txm = props.transactionManager(emf!!)
    }

    fun entityInterceptor(): OrmInterceptor =
        OrmInterceptor(session, time)

    /** トランザクション処理を行います。  */
    fun <T> tx(callable: () -> T): T? {
        return TransactionTemplate(txm!!).execute<T> {
            val ret = callable()
            if (ret is Entity) {
                ret.hashCode() // for lazy loading
            }
            ret
        }
    }

}

// 簡易コンポーネントFactory
object EntityTestFactory {
    var ds: Optional<DataSource> = Optional.empty()

    fun createDataSource(): DataSource {
        val ds = OrmDataSourceProperties()
        ds.url = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
        ds.username = ""
        ds.password = ""
        return ds.dataSource()
    }

    fun dataSource(): DataSource =
            ds.orElseGet({
                ds = Optional.of(createDataSource())
                ds.get()
            })
}
