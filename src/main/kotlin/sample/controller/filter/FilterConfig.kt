package sample.controller.filter

import org.springframework.context.annotation.Configuration
import sample.context.security.SecurityFilters
import java.util.ArrayList
import javax.servlet.Filter

/**
 * ServletFilterの拡張実装。
 * filtersで返すFilterはSecurityHandlerにおいてActionSessionFilterの後に定義されます。
 */
@Configuration
class FilterConfig : SecurityFilters {

    override fun filters(): List<Filter> = listOf()

}