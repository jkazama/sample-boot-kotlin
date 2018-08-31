package sample.model

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import sample.ActionStatusType
import sample.context.AppSetting
import sample.context.Timestamper
import sample.context.orm.DefaultRepository
import sample.context.orm.SystemRepository
import sample.model.account.Account
import sample.model.account.AccountStatusType
import sample.model.account.FiAccount
import sample.model.account.Login
import sample.model.asset.*
import sample.model.master.Holiday
import sample.model.master.SelfFiAccount
import sample.model.master.Staff
import sample.model.master.StaffAuthority
import sample.util.DateUtils
import sample.util.TimePoint
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import javax.annotation.PostConstruct

/**
 * データ生成用のサポートコンポーネント。
 *
 * テストや開発時の簡易マスタデータ生成を目的としているため本番での利用は想定していません。
 */
@Component
@ConditionalOnProperty(prefix = "extension.datafixture", name = ["enabled"], matchIfMissing = false)
class DataFixtures(
        val time: Timestamper,
        val businessDay: BusinessDayHandler,
        val encoder: PasswordEncoder,
        val rep: DefaultRepository,
        @Qualifier(DefaultRepository.BeanNameTx)
        val tx: PlatformTransactionManager,
        val repSystem: SystemRepository,
        @Qualifier(SystemRepository.BeanNameTx)
        val txSystem: PlatformTransactionManager) {

    @PostConstruct
    fun initialize() {
        TransactionTemplate(txSystem).execute {
            initializeInTxSystem()
        }
        TransactionTemplate(tx).execute {
            initializeInTx()
        }
    }

    fun initializeInTxSystem() {
        val day = DateUtils.dayFormat(LocalDate.now())
        AppSetting(Timestamper.KeyDay, "system", "営業日", day).save(repSystem)
    }

    fun initializeInTx() {
        val ccy = "JPY"
        val baseDay = businessDay.day()

        // 社員: admin (passも同様)
        staff("admin").save(rep)

        // 自社金融機関
        selfFiAcc(Remarks.CashOut, ccy).save(rep)

        // 口座: sample (passも同様)
        val idSample = "sample"
        acc(idSample).save(rep)
        login(idSample).save(rep)
        fiAcc(idSample, Remarks.CashOut, ccy).save(rep)
        cb(idSample, baseDay, ccy, "1000000").save(rep)
    }

    // account

    /** 口座の簡易生成  */
    fun acc(id: String): Account =
            Account(
                    id = id,
                    name = id,
                    mail = "hoge@example.com",
                    statusType = AccountStatusType.Normal
            )

    fun login(id: String): Login =
            Login(
                    id = id,
                    loginId = id,
                    password = encoder.encode(id)
            )

    /** 口座に紐付く金融機関口座の簡易生成  */
    fun fiAcc(accountId: String, category: String, currency: String): FiAccount =
            FiAccount(
                    accountId = accountId,
                    category = category,
                    currency = currency,
                    fiCode = "$category-$currency",
                    fiAccountId = "FI$accountId"
            )

    // asset

    /** 口座残高の簡易生成  */
    fun cb(accountId: String, baseDay: LocalDate, currency: String, amount: String): CashBalance =
            CashBalance(null, accountId, baseDay, currency, BigDecimal(amount), LocalDateTime.now())

    /** キャッシュフローの簡易生成  */
    fun cf(accountId: String, amount: String, eventDay: LocalDate, valueDay: LocalDate): Cashflow =
            cfReg(accountId, amount, valueDay).create(TimePoint(eventDay))

    /** キャッシュフロー登録パラメタの簡易生成  */
    fun cfReg(accountId: String, amount: String, valueDay: LocalDate): RegCashflow =
            RegCashflow(accountId, "JPY", BigDecimal(amount), CashflowType.CashIn, "cashIn", null, valueDay)

    /** 振込入出金依頼の簡易生成 [発生日(T+1)/受渡日(T+3)]  */
    fun cio(accountId: String, absAmount: String, withdrawal: Boolean): CashInOut {
        val now = time.tp()
        return CashInOut(
                accountId = accountId,
                currency = "JPY",
                absAmount = BigDecimal(absAmount),
                withdrawal = withdrawal,
                requestDay = now.day,
                requestDate = now.date,
                eventDay = businessDay.day(1),
                valueDay = businessDay.day(3),
                targetFiCode = "tFiCode",
                targetFiAccountId = "tFiAccId",
                selfFiCode = "sFiCode",
                selfFiAccountId = "sFiAccId",
                statusType = ActionStatusType.Unprocessed
        )
    }

    // master

    /** 社員の簡易生成  */
    fun staff(id: String): Staff =
            Staff(
                    id = id,
                    name = id,
                    password = encoder.encode(id)
            )

    /** 社員権限の簡易生成  */
    fun staffAuth(id: String, vararg authority: String): List<StaffAuthority> =
            authority.map { StaffAuthority(staffId = id, authority = it) }

    /** 自社金融機関口座の簡易生成  */
    fun selfFiAcc(category: String, currency: String): SelfFiAccount =
            SelfFiAccount(
                    category = category,
                    currency = currency,
                    fiCode = "$category-$currency",
                    fiAccountId = "xxxxxx"
            )

    /** 祝日の簡易生成  */
    fun holiday(dayStr: String): Holiday =
            Holiday(
                    category = Holiday.CategoryDefault,
                    name = "休日サンプル",
                    day = DateUtils.day(dayStr)
            )
}
