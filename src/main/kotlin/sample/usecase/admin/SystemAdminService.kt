package sample.usecase.admin

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Transactional
import sample.context.AppSetting
import sample.context.DomainHelper
import sample.context.FindAppSetting
import sample.context.audit.*
import sample.context.orm.SystemRepository
import sample.context.orm.PagingList
import sample.context.orm.DefaultRepository
import sample.model.BusinessDayHandler

/**
 * システムドメインに対する社内ユースケース処理。
 */
@Service
class SystemAdminService(
        private val dh: DomainHelper,
        private val rep: SystemRepository,
        private val audit: AuditHandler,
        private val businessDay: BusinessDayHandler
) {

    /** 利用者監査ログを検索します。  */
    @Transactional(SystemRepository.BeanNameTx)
    fun findAuditActor(p: FindAuditActor): PagingList<AuditActor> = AuditActor.find(rep, p)

    /** イベント監査ログを検索します。  */
    @Transactional(SystemRepository.BeanNameTx)
    fun findAuditEvent(p: FindAuditEvent): PagingList<AuditEvent> = AuditEvent.find(rep, p)

    /** アプリケーション設定一覧を検索します。  */
    @Transactional(SystemRepository.BeanNameTx)
    fun findAppSetting(p: FindAppSetting): List<AppSetting> = AppSetting.find(rep, p)

    fun changeAppSetting(id: String, value: String) =
        audit.audit("アプリケーション設定情報を変更する") {
            dh.settingSet(id, value)
        }

    fun processDay() =
        audit.audit("営業日を進める") {
            dh.time.proceedDay(businessDay.day(1))
        }

}