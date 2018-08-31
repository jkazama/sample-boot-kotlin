package sample

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.hibernate5.Hibernate5Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.springframework.boot.actuate.health.AbstractHealthIndicator
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.context.MessageSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean
import sample.context.AppSettingHandler
import sample.context.DomainHelper
import sample.context.ResourceBundleHandler
import sample.context.Timestamper
import sample.context.actor.ActorSession
import sample.context.audit.AuditHandler
import sample.context.audit.AuditPersister
import sample.context.lock.IdLockHandler
import sample.context.mail.MailHandler
import sample.context.orm.DefaultRepository
import sample.context.orm.SystemRepository
import sample.context.report.ReportHandler
import sample.model.BusinessDayHandler
import sample.model.DataFixtures
import sample.model.HolidayAccessor

/**
 * アプリケーションにおけるBean定義を表現します。
 * <p>controller / usecase 以外のコンポーネントはこちらで明示的に定義しています。
 * <p>依存コンポーネントが多いものについては Import を併用しています。
 */
@Configuration
class ApplicationConfig {

    /** インフラ層 ( context 配下) のコンポーネント定義を表現します  */
    @Configuration
    @Import(DomainHelper::class)
    internal class PlainConfig {
        @Bean
        fun timestamper(): Timestamper = Timestamper()

        @Bean
        fun actorSession(): ActorSession = ActorSession()

        @Bean
        fun resourceBundleHandler(): ResourceBundleHandler = ResourceBundleHandler()

        @Bean
        fun appSettingHandler(rep: SystemRepository): AppSettingHandler = AppSettingHandler(rep)

        @Bean
        fun auditHandler(session: ActorSession,  persister: AuditPersister): AuditHandler = AuditHandler(session, persister)

        @Bean
        fun auditPersister(rep: SystemRepository): AuditPersister = AuditPersister(rep)

        @Bean
        fun idLockHandler(): IdLockHandler = IdLockHandler()

        @Bean
        fun mailHandler(): MailHandler = MailHandler()

        @Bean
        fun reportHandler(): ReportHandler = ReportHandler()

    }

    /** ドメイン層 ( model 配下) のコンポーネント定義を表現します  */
    @Configuration
    @Import(BusinessDayHandler::class, DataFixtures::class)
    internal class DomainConfig {

        @Bean
        fun holidayAccessor(rep: DefaultRepository): HolidayAccessor =
                HolidayAccessor(rep)
    }

    @Configuration
    internal class WebMVCConfig {

        /** HibernateのLazyLoading回避対応。  see JacksonAutoConfiguration  */
        @Bean
        fun jsonHibernate5Module(): Hibernate5Module = Hibernate5Module()

        /** BeanValidationメッセージのUTF-8に対応したValidator。  */
        @Bean
        fun defaultValidator(message: MessageSource): LocalValidatorFactoryBean {
            val factory = LocalValidatorFactoryBean()
            factory.setValidationMessageSource(message)
            return factory
        }
    }

}