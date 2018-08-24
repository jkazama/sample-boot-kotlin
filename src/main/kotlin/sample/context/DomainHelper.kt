package sample.context

import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import sample.context.actor.Actor
import sample.context.actor.ActorSession


/**
 * ドメイン処理を行う上で必要となるインフラ層コンポーネントへのアクセサを提供します。
 */
open class DomainHelper(
        /** スレッドローカルスコープの利用者セッション  */
        val actorSession: ActorSession,
        /** 日時ユーティリティ */
        val time: Timestamper,
        val settingHandler: ObjectProvider<AppSettingHandler>
) {

    /** ログイン中のユースケース利用者を取得します。  */
    fun actor(): Actor = actorSession.actor()

    /** アプリケーション設定情報を取得します。  */
    fun setting(id: String): AppSetting = settingHandler.getObject().setting(id)

    /** アプリケーション設定情報を設定します。  */
    fun settingSet(id: String, value: String): AppSetting = settingHandler.getObject().update(id, value)

}