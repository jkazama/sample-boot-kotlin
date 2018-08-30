package sample.usecase.admin

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Transactional
import sample.context.DomainHelper
import sample.context.audit.AuditHandler
import sample.context.orm.DefaultRepository
import sample.model.master.Holiday
import sample.model.master.RegHoliday
import sample.model.master.Staff
import sample.model.master.StaffAuthority
import sample.usecase.ServiceUtils
import java.util.*


/**
 * サービスマスタドメインに対する社内ユースケース処理。
 */
@Service
class MasterAdminService(
        private val rep: DefaultRepository,
        @Qualifier(DefaultRepository.BeanNameTx)
        private val txm: PlatformTransactionManager,
        private val audit: AuditHandler
) {

    /** 社員を取得します。  */
    @Transactional(DefaultRepository.BeanNameTx)
    @Cacheable("MasterAdminService.getStaff")
    fun getStaff(id: String): Optional<Staff> = Staff.get(rep, id)

    /** 社員権限を取得します。  */
    @Transactional(DefaultRepository.BeanNameTx)
    @Cacheable("MasterAdminService.findStaffAuthority")
    fun findStaffAuthority(staffId: String): List<StaffAuthority> = StaffAuthority.find(rep, staffId)

    fun registerHoliday(p: RegHoliday) =
            audit.audit("休日情報を登録する") {
                ServiceUtils.tx(txm) {
                    Holiday.register(rep, p)
                }
            }

}