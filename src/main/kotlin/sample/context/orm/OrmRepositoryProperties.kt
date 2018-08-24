package sample.context.orm

import org.apache.commons.lang3.ArrayUtils
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties
import org.springframework.orm.jpa.JpaTransactionManager
import javax.persistence.EntityManagerFactory
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter
import org.springframework.orm.jpa.JpaVendorAdapter
import org.springframework.boot.autoconfigure.orm.jpa.HibernateSettings
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder
import org.terracotta.statistics.StatisticsManager.properties
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import javax.sql.DataSource


/** JPA コンポーネントを生成するための設定情報を表現します。 */
class OrmRepositoryProperties(
        /** スキーマ紐付け対象とするパッケージ。(annotatedClassesとどちらかを設定) */
        var packageToScan: Array<String>? = null,
        /** Entityとして登録するクラス。(packageToScanとどちらかを設定) */
        var annotatedClasses: Array<Class<*>>? = null
) : JpaProperties() {

    fun entityManagerFactoryBean(name: String, dataSource: DataSource): LocalContainerEntityManagerFactoryBean {
        val emfBuilder = EntityManagerFactoryBuilder(
                vendorAdapter(), properties, null)
        val builder = emfBuilder
                .dataSource(dataSource)
                .persistenceUnit(name)
                .properties(getHibernateProperties(HibernateSettings()))
                .jta(false)
        if (ArrayUtils.isNotEmpty(annotatedClasses)) {
            builder.packages(*annotatedClasses.orEmpty())
        } else {
            builder.packages(*packageToScan.orEmpty())
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