package sample.context

import org.springframework.stereotype.Component
import sample.context.actor.Actor
import sample.context.actor.ActorSession


/**
 * ドメイン処理を行う上で必要となるインフラ層コンポーネントへのアクセサを提供します。
 */
@Component
class DomainHelper(
        /** スレッドローカルスコープの利用者セッション  */
        val actorSession: ActorSession,
        /** 日時ユーティリティ */
        val time: Timestamper,
        private val settingHandler: AppSettingHandler
) {

    /** ログイン中のユースケース利用者を取得します。  */
    fun actor(): Actor = actorSession.actor()

    /** アプリケーション設定情報を取得します。  */
    fun setting(id: String): AppSetting = settingHandler.setting(id)

    /** アプリケーション設定情報を設定します。  */
    fun settingSet(id: String, value: String): AppSetting = settingHandler.update(id, value)

}