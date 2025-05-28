package sample

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Import
import sample.controller.AccountController
import sample.usecase.AccountService

/**
 * Application process startup class. <p>By executing this class, the application startup with the
 * embedded Tomcat provided by SpringBoot is performed. <p>Component scanning is performed only
 * under the controller / usecase packages.
 */
@SpringBootApplication(scanBasePackageClasses = [AccountController::class, AccountService::class])
@EnableCaching(proxyTargetClass = true)
@Import(ApplicationConfig::class, ApplicationDbConfig::class, ApplicationSecurityConfig::class)
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
