package sample.context.orm

import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.stereotype.Repository
import sample.context.DomainHelper
import sample.context.orm.DefaultRepository.Companion.BeanNameEmf
import javax.persistence.EntityManager
import javax.persistence.EntityManagerFactory
import javax.persistence.PersistenceContext
import javax.sql.DataSource


/** 標準スキーマのRepositoryを表現します。 */
@Repository
class DefaultRepository(
        dh: ObjectProvider<DomainHelper>,
        interceptor: ObjectProvider<OrmInterceptor>,
        @PersistenceContext(unitName = BeanNameEmf)
        var em: EntityManager? = null
) : OrmRepository(dh, interceptor) {

    override fun em(): EntityManager = em!!

    companion object {
        const val BeanNameDs = "dataSource"
        const val BeanNameEmf = "entityManagerFactory"
        const val BeanNameTx = "transactionManager"
    }

}

/** 標準スキーマのDataSourceを生成します。  */
@ConfigurationProperties(prefix = "extension.datasource.default")
class DefaultDataSourceProperties(
        var jpa: OrmRepositoryProperties = OrmRepositoryProperties()
) : OrmDataSourceProperties() {

    fun entityManagerFactoryBean(dataSource: DataSource): LocalContainerEntityManagerFactoryBean =
            jpa.entityManagerFactoryBean(BeanNameEmf, dataSource)

    fun transactionManager(emf: EntityManagerFactory): JpaTransactionManager =
            jpa.transactionManager(emf)
}
