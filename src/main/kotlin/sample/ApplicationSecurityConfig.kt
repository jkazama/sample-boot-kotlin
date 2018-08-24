package sample

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import sample.context.security.SecurityProperties

/**
 * アプリケーションのセキュリティ定義を表現します。
 */
@Configuration
@EnableConfigurationProperties(SecurityProperties::class)
class ApplicationSecurityConfig {

    /**
     * パスワード用のハッシュ(BCrypt)エンコーダー。
     * low: きちんとやるのであれば、strengthやSecureRandom使うなど外部切り出し含めて検討してください
     */
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

}