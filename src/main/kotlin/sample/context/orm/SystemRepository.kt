package sample.context.orm

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.stereotype.Repository
import sample.context.DomainHelper
import sample.context.orm.SystemRepository.Companion.BeanNameEmf
import javax.persistence.EntityManager
import javax.persistence.EntityManagerFactory
import javax.persistence.PersistenceContext
import javax.sql.DataSource


/** システムスキーマのRepositoryを表現します。 */
@Repository
class SystemRepository(
        dh: DomainHelper,
        interceptor: OrmInterceptor? = null,
        @PersistenceContext(unitName = BeanNameEmf)
        var em: EntityManager? = null
) : OrmRepository(dh, interceptor) {

    override fun em(): EntityManager = em!!

    companion object {
        const val BeanNameDs = "systemDataSource"
        const val BeanNameEmf = "systemEntityManagerFactory"
        const val BeanNameTx = "systemTransactionManager"
    }

}

/** システムスキーマのDataSourceを生成します。  */
@ConfigurationProperties(prefix = "extension.datasource.system")
class SystemDataSourceProperties(
        var jpa: OrmRepositoryProperties = OrmRepositoryProperties()
) : OrmDataSourceProperties() {

    fun entityManagerFactoryBean(dataSource: DataSource): LocalContainerEntityManagerFactoryBean =
            jpa.entityManagerFactoryBean(BeanNameEmf, dataSource)

    fun transactionManager(emf: EntityManagerFactory): JpaTransactionManager =
            jpa.transactionManager(emf)

}
