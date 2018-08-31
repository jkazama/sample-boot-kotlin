package sample.support

import sample.context.AppSettingHandler
import sample.context.DomainHelper
import sample.context.SimpleObjectProvider
import sample.context.Timestamper
import sample.context.actor.ActorSession
import java.time.Clock
import java.util.*


/** モックテスト用のドメインヘルパー  */
class MockDomainHelper(
        val mockClock: Clock = Clock.systemDefaultZone(),
        val settingMap: MutableMap<String, String> = mutableMapOf()
) : DomainHelper(
        ActorSession(), Timestamper(mockClock), AppSettingHandler(mockMap = Optional.of(settingMap))
) {
    fun setting(id: String, value: String): MockDomainHelper {
        settingMap[id] = value
        return this
    }
}
