package sample.context.orm

import com.zaxxer.hikari.HikariDataSource
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.boot.jdbc.DatabaseDriver
import java.util.*
import javax.sql.DataSource

/**
 * DataSource生成用の設定クラス。
 * <p>継承先で@ConfigurationProperties定義を行ってapplication.ymlと紐付してください。
 * <p>ベース実装にHikariCPを利用しています。必要に応じて設定可能フィールドを増やすようにしてください。
 */
open class OrmDataSourceProperties(
        /** ドライバクラス名称 ( 未設定時は url から自動登録 )  */
        var driverClassName: String? = null,
        var url: String? = null,
        var username: String? = null,
        var password: String? = null,
        var props: Properties = Properties(),
        /** 最低接続プーリング数  */
        var minIdle: Int = 1,
        /** 最大接続プーリング数  */
        var maxPoolSize: Int = 20,
        /** コネクション状態を確認する時は true  */
        var validation: Boolean = true,
        /** コネクション状態確認クエリ ( 未設定時かつ Database が対応している時は自動設定 )  */
        var validationQuery: String? = null
) {
    val name: String
        get() = this.javaClass.simpleName.replace("Properties".toRegex(), "")

    open fun dataSource(): DataSource {
        val dataSource = DataSourceBuilder.create()
                .type(HikariDataSource::class.java)
                .driverClassName(this.driverClassName()).url(this.url)
                .username(this.username).password(this.password)
                .build() as HikariDataSource
        dataSource.minimumIdle = minIdle
        dataSource.maximumPoolSize = maxPoolSize
        if (validation) {
            dataSource.connectionTestQuery = validationQuery()
        }
        dataSource.poolName = name
        dataSource.dataSourceProperties = props
        return dataSource
    }

    private fun driverClassName(): String =
            if (driverClassName.isNullOrBlank()) {
                DatabaseDriver.fromJdbcUrl(url).driverClassName
            } else driverClassName.orEmpty()

    private fun validationQuery(): String =
            if (validationQuery.isNullOrBlank()) {
                DatabaseDriver.fromJdbcUrl(url).validationQuery
            } else validationQuery.orEmpty()
}