package sample.context.orm

import java.util.regex.Pattern
import java.util.regex.Pattern.CASE_INSENSITIVE
import java.util.regex.Pattern.compile
import org.apache.commons.lang3.StringUtils.replaceFirst
import org.springframework.data.jpa.repository.support.JpaEntityInformationSupport
import java.io.Serializable
import org.springframework.data.jpa.repository.support.JpaEntityInformation
import org.springframework.util.Assert
import org.springframework.util.StringUtils
import javax.persistence.EntityManager



/**
 * Orm 関連のユーティリティを提供します。
 */
object OrmUtils {

    private val IDENTIFIER = "[._[\\P{Z}&&\\P{Cc}&&\\P{Cf}&&\\P{P}]]+"
    private val IDENTIFIER_GROUP = String.format("(%s)", IDENTIFIER)
    private val VARIABLE_NAME_GROUP_INDEX = 4
    private val SIMPLE_COUNT_VALUE = "$2"
    private val COMPLEX_COUNT_VALUE = "$3$6"
    private val COUNT_REPLACEMENT_TEMPLATE = "select count(%s) $5$6$7"
    private val ORDER_BY_PART = "(?iu)\\s+order\\s+by\\s+.*$"
    private val COUNT_MATCH: Pattern
        get() {
            val builder = StringBuilder()
            builder.append("(select\\s+((distinct )?(.+?)?)\\s+)?(from\\s+")
            builder.append(IDENTIFIER)
            builder.append("(?:\\s+as)?\\s+)")
            builder.append(IDENTIFIER_GROUP)
            builder.append("(.*)")
            return compile(builder.toString(), CASE_INSENSITIVE)
        }

    /** 指定したクラスのエンティティ情報を返します ( ID 概念含む )  */
    @Suppress("UNCHECKED_CAST")
    fun <T> entityInformation(em: EntityManager, clazz: Class<T>): JpaEntityInformation<T, Serializable?> =
        JpaEntityInformationSupport.getEntityInformation(clazz, em) as JpaEntityInformation<T, Serializable?>

    /** カウントクエリを生成します。 see QueryUtils#createCountQueryFor  */
    fun createCountQueryFor(originalQuery: String): String {
        Assert.hasText(originalQuery, "OriginalQuery must not be null or empty!")
        val matcher = COUNT_MATCH.matcher(originalQuery)
        val variable = if (matcher.matches()) matcher.group(VARIABLE_NAME_GROUP_INDEX) else null
        val useVariable = (variable != null && StringUtils.hasText(variable) && !variable.startsWith("new")
                && !variable.startsWith("count(") && !variable.contains(","))

        val replacement = if (useVariable) SIMPLE_COUNT_VALUE else COMPLEX_COUNT_VALUE
        val countQuery = matcher.replaceFirst(String.format(COUNT_REPLACEMENT_TEMPLATE, replacement))
        return countQuery.replaceFirst(ORDER_BY_PART.toRegex(), "")
    }

}