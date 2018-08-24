package sample.context

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.support.ResourceBundleMessageSource
import java.util.*
import java.util.Locale


/**
 * ResourceBundleに対する簡易アクセスを提供します。
 * <p>本コンポーネントはAPI経由でのラベル一覧の提供等、i18n用途のメッセージプロパティで利用してください。
 * <p>ResourceBundleは単純な文字列変換を目的とする標準のMessageSourceとは異なる特性(リスト概念)を
 * 持つため、別インスタンスでの管理としています。
 * （spring.messageとは別に指定[extension.messages]する必要があるので注意してください）
 */
@ConfigurationProperties(prefix = "extension.messages")
open class ResourceBundleHandler(val encoding: String = "UTF-8") {
    private val bundleMap: MutableMap<String, ResourceBundle> = mutableMapOf()

    /**
     * 指定されたメッセージソースのResourceBundleを返します。
     *
     * basenameに拡張子(.properties)を含める必要はありません。
     */
    fun get(basename: String): ResourceBundle =
        get(basename, Locale.getDefault())

    @Synchronized
    fun get(basename: String, locale: Locale): ResourceBundle {
        bundleMap.putIfAbsent(keyname(basename, locale), ResourceBundleFactory.create(basename, locale, encoding))
        return bundleMap[keyname(basename, locale)]!!
    }

    private fun keyname(basename: String, locale: Locale): String =
        basename + "_" + locale.toLanguageTag()

    /**
     * 指定されたメッセージソースのラベルキー、値のMapを返します。
     *
     * basenameに拡張子(.properties)を含める必要はありません。
     */
    fun labels(basename: String): Map<String, String> =
        labels(basename, Locale.getDefault())

    fun labels(basename: String, locale: Locale): Map<String, String> {
        val bundle = get(basename, locale)
        return bundle.keySet().associate { Pair(it, bundle.getString(it)) }
    }

}

/**
 * SpringのMessageSource経由でResourceBundleを取得するFactory。
 *
 * プロパティファイルのエンコーディング指定を可能にしています。
 */
class ResourceBundleFactory : ResourceBundleMessageSource() {
    companion object {
        /** ResourceBundleを取得します。  */
        fun create(basename: String, locale: Locale, encoding: String): ResourceBundle {
            val factory = ResourceBundleFactory()
            factory.defaultEncoding = encoding
            return Optional.ofNullable(factory.getResourceBundle(basename, locale))
                    .orElseThrow { IllegalArgumentException("指定されたbasenameのリソースファイルは見つかりませんでした。[]") }
        }
    }
}

