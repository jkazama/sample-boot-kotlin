package sample

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.core.annotation.Order
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter
import sample.context.security.*

/** Represents the security definition of the application. */
@Configuration
@EnableConfigurationProperties(SecurityProperties::class)
class ApplicationSecurityConfig {

    /**
     * Password hash (BCrypt) encoder. low: For proper implementation, consider external extraction
     * including strength and SecureRandom usage
     */
    @Bean fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    /** CORS global application */
    @Bean
    @ConditionalOnProperty(
            prefix = "extension.security.cors",
            name = arrayOf("enabled"),
            matchIfMissing = false
    )
    fun corsFilter(props: SecurityProperties): CorsFilter {
        val source = UrlBasedCorsConfigurationSource()
        val config = CorsConfiguration()
        config.allowCredentials = props.cors.allowCredentials
        config.addAllowedOriginPattern(props.cors.allowedOrigin)
        config.addAllowedHeader(props.cors.allowedHeader)
        config.addAllowedMethod(props.cors.allowedMethod)
        config.maxAge = props.cors.maxAge
        source.registerCorsConfiguration(props.cors.path, config)
        return CorsFilter(source)
    }

    /** Represents API authentication/authorization definition using Spring Security. */
    @Configuration
    @EnableWebSecurity
    @EnableMethodSecurity(prePostEnabled = true)
    @Import(
            SecurityConfigurer::class,
            SecurityProvider::class,
            SecurityEntryPoint::class,
            LoginHandler::class,
            SecurityActorFinder::class
    )
    @Order(org.springframework.boot.autoconfigure.security.SecurityProperties.BASIC_AUTH_ORDER)
    internal class AuthSecurityConfig {

        /** Custom authentication process management component for Spring Security. */
        @Bean
        fun authenticationManager(
                http: HttpSecurity,
                encoder: PasswordEncoder,
                securityProvider: SecurityProvider
        ): AuthenticationManager {
            return http.getSharedObject(
                            org.springframework.security.config.annotation.authentication.builders
                                            .AuthenticationManagerBuilder::class
                                    .java
                    )
                    .authenticationProvider(securityProvider)
                    .build()
        }
    }
}
