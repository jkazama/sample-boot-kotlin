package sample.context.orm

import org.springframework.boot.autoconfigure.orm.jpa.HibernateProperties
import org.springframework.boot.autoconfigure.orm.jpa.HibernateSettings
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.JpaVendorAdapter
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter
import javax.persistence.EntityManagerFactory
import javax.sql.DataSource


/** JPA コンポーネントを生成するための設定情報を表現します。 */
data class OrmRepositoryProperties(
        /** スキーマ紐付け対象とするパッケージ。(annotatedClassesとどちらかを設定) */
        var packageToScan: Collection<String> = listOf(),
        /** Entityとして登録するクラス。(packageToScanとどちらかを設定) */
        var annotatedClasses: Collection<Class<*>> = listOf(),
        var hibernate: HibernateProperties = HibernateProperties()
) : JpaProperties() {

    fun entityManagerFactoryBean(name: String, dataSource: DataSource): LocalContainerEntityManagerFactoryBean {
        val emfBuilder = EntityManagerFactoryBuilder(
                vendorAdapter(), properties, null)
        val builder = emfBuilder
                .dataSource(dataSource)
                .persistenceUnit(name)
                .properties(hibernate.determineHibernateProperties(getProperties(), HibernateSettings()))
                .jta(false)
        if (annotatedClasses.isNotEmpty()) {
            builder.packages(*annotatedClasses.toTypedArray())
        } else {
            builder.packages(*packageToScan.toTypedArray())
        }
        return builder.build()
    }

    private fun vendorAdapter(): JpaVendorAdapter {
        val adapter = HibernateJpaVendorAdapter()
        adapter.setShowSql(isShowSql)
        if (database != null) {
            adapter.setDatabase(database)
        }
        adapter.setDatabasePlatform(databasePlatform)
        adapter.setGenerateDdl(isGenerateDdl)
        return adapter
    }

    fun transactionManager(emf: EntityManagerFactory): JpaTransactionManager =
            JpaTransactionManager(emf)

}