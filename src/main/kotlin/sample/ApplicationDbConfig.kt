package sample

import org.springframework.beans.factory.ObjectProvider
import javax.persistence.EntityManagerFactory
import sample.context.orm.SystemRepository
import org.springframework.beans.factory.annotation.Qualifier
import sample.context.orm.SystemDataSourceProperties
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import sample.context.orm.DefaultRepository
import sample.context.orm.DefaultDataSourceProperties
import org.springframework.context.annotation.Primary
import sample.context.orm.OrmInterceptor
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import sample.context.DomainHelper
import sample.context.Timestamper
import sample.context.actor.ActorSession
import javax.sql.DataSource


/**
 * アプリケーションのデータベース接続定義を表現します。
 */
@Configuration
@EnableConfigurationProperties(DefaultDataSourceProperties::class, SystemDataSourceProperties::class)
class ApplicationDbConfig {

    /** 永続化時にメタ情報の差込を行うインターセプタ  */
    @Bean
    internal fun ormInterceptor(session: ActorSession, time: Timestamper): OrmInterceptor =
        OrmInterceptor(session = session, time = time)

    /** 標準スキーマへの接続定義を表現します。  */
    @Configuration
    internal class DefaultDbConfig {

        @Bean
        fun defaultRepository(dh: ObjectProvider<DomainHelper>, interceptor: ObjectProvider<OrmInterceptor>): DefaultRepository =
            DefaultRepository(dh, interceptor)

        @Bean(name = [DefaultRepository.BeanNameDs], destroyMethod = "close")
        @Primary
        fun dataSource(props: DefaultDataSourceProperties): DataSource =
            props.dataSource()

        @Bean(name = [DefaultRepository.BeanNameEmf])
        @Primary
        fun entityManagerFactoryBean(
                props: DefaultDataSourceProperties,
                @Qualifier(DefaultRepository.BeanNameDs) dataSource: DataSource): LocalContainerEntityManagerFactoryBean =
            props.entityManagerFactoryBean(dataSource)

        @Bean(name = [DefaultRepository.BeanNameTx])
        @Primary
        fun transactionManager(
                props: DefaultDataSourceProperties,
                @Qualifier(DefaultRepository.BeanNameEmf) emf: EntityManagerFactory): JpaTransactionManager =
            props.transactionManager(emf)

    }

    /** システムスキーマへの接続定義を表現します。  */
    @Configuration
    internal class SystemDbConfig {

        @Bean
        fun systemRepository(dh: ObjectProvider<DomainHelper>, interceptor: ObjectProvider<OrmInterceptor>): SystemRepository =
            SystemRepository(dh, interceptor)

        @Bean(name = [SystemRepository.BeanNameDs], destroyMethod = "close")
        fun systemDataSource(props: SystemDataSourceProperties): DataSource =
            props.dataSource()

        @Bean(name = [SystemRepository.BeanNameEmf])
        fun systemEntityManagerFactoryBean(
                props: SystemDataSourceProperties,
                @Qualifier(SystemRepository.BeanNameDs) dataSource: DataSource): LocalContainerEntityManagerFactoryBean =
            props.entityManagerFactoryBean(dataSource)

        @Bean(name = [SystemRepository.BeanNameTx])
        fun systemTransactionManager(
                props: SystemDataSourceProperties,
                @Qualifier(SystemRepository.BeanNameEmf) emf: EntityManagerFactory): JpaTransactionManager =
            props.transactionManager(emf)

    }

}
