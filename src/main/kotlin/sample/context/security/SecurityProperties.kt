package sample.context.security

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * セキュリティ関連の設定情報を表現します。
 */
@ConfigurationProperties(prefix = "extension.security")
data class SecurityProperties(
        /** Spring Security依存の認証/認可設定情報  */
        var auth: SecurityAuthProperties = SecurityAuthProperties(),
        /** CORS設定情報  */
        var cors: SecurityCorsProperties = SecurityCorsProperties()
)

/** Spring Securityに対する拡張設定情報  */
data class SecurityAuthProperties(
        /** リクエスト時のログインIDを取得するキー  */
        var loginKey: String = "loginId",
        /** リクエスト時のパスワードを取得するキー  */
        var passwordKey: String = "password",
        /** 認証対象パス  */
        var path: List<String> = listOf("/api/**"),
        /** 認証対象パス(管理者向け)  */
        var pathAdmin: List<String> = listOf("/api/admin/**"),
        /** 認証除外パス(認証対象からの除外)  */
        var excludesPath: List<String> = listOf("/api/system/job/**"),
        /** 認証無視パス(フィルタ未適用の認証未考慮、静的リソース等)  */
        var ignorePath: List<String> = listOf("/css/**", "/js/**", "/img/**", "/**/favicon.ico"),
        /** ログインAPIパス  */
        var loginPath: String = "/api/login",
        /** ログアウトAPIパス  */
        var logoutPath: String = "/api/logout",
        /** 一人が同時利用可能な最大セッション数  */
        var maximumSessions: Int = 2,
        /**
         * 社員向けモードの時はtrue。
         *
         * ログインパスは同じですが、ログイン処理の取り扱いが切り替わります。
         *
         *  * true: SecurityUserService
         *  * false: SecurityAdminService
         *
         */
        var admin: Boolean = false,
        /** 認証が有効な時はtrue  */
        var enabled: Boolean = true
)

/** CORS設定情報を表現します。  */
data class SecurityCorsProperties(
        var allowCredentials: Boolean = true,
        var allowedOrigin: String = "*",
        var allowedHeader: String = "*",
        var allowedMethod: String = "*",
        var maxAge: Long = 3600L,
        var path: String = "/**"
)
