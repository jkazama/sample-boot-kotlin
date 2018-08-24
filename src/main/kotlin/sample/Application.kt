package sample

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Import

/**
 * アプリケーションプロセスの起動クラス。
 * <p>本クラスを実行する事でSpringBootが提供する組込Tomcatでのアプリケーション起動が行われます。
 */
@SpringBootApplication(scanBasePackageClasses = [])
@EnableCaching(proxyTargetClass = true)
@Import(ApplicationConfig::class, ApplicationDbConfig::class, ApplicationSecurityConfig::class)
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
