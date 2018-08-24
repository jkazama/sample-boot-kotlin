package sample

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.boot.runApplication

/**
 * アプリケーションプロセスの起動クラス。
 * <p>本クラスを実行する事でSpringBootが提供する組込Tomcatでのアプリケーション起動が行われます。
 */
@SpringBootApplication
@EnableCaching(proxyTargetClass = true)
open class Application {

fun main(args: Array<String>) {
    System.out.println("Hello")
}
}
