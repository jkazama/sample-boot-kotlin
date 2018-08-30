package sample.context

import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import sample.context.orm.SystemRepository
import java.util.*

/**
 * アプリケーション設定情報に対するアクセス手段を提供します。
 */
@Component
class AppSettingHandler(
        private val rep: SystemRepository? = null,
        private val mockMap: Optional<MutableMap<String, String>> = Optional.empty()
) {
    /** アプリケーション設定情報を取得します。  */
    @Cacheable(cacheNames = ["AppSettingHandler.appSetting"], key = "#id")
    @Transactional(value = SystemRepository.BeanNameTx)
    fun setting(id: String): AppSetting {
        if (mockMap.isPresent) {
            return mockSetting(id)
        }
        val setting = AppSetting.load(rep!!, id)
        setting.hashCode() // for loading
        return setting
    }

    private fun mockSetting(id: String): AppSetting =
        AppSetting(id, "category", "テスト用モック情報", mockMap.get()[id].orEmpty())

    /** アプリケーション設定情報を変更します。  */
    @CacheEvict(cacheNames = ["AppSettingHandler.appSetting"], key = "#id")
    @Transactional(value = SystemRepository.BeanNameTx)
    fun update(id: String, value: String): AppSetting =
        if (mockMap.isPresent) mockSetting(id) else AppSetting.load(rep!!, id).update(rep, value)

}