package sample

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Import
import sample.controller.AccountController
import sample.usecase.AccountService

/**
 * アプリケーションプロセスの起動クラス。
 * <p>本クラスを実行する事でSpringBootが提供する組込Tomcatでのアプリケーション起動が行われます。
 * <p>controller / usecase パッケージ配下のみコンポーネントスキャンをおこないます。
 */
@SpringBootApplication(scanBasePackageClasses = [
    AccountController::class, AccountService::class
])
@EnableCaching(proxyTargetClass = true)
@Import(ApplicationConfig::class, ApplicationDbConfig::class, ApplicationSecurityConfig::class)
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
