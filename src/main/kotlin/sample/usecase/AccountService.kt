package sample.usecase

import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import sample.context.orm.DefaultRepository
import sample.model.account.Account
import sample.model.account.Login
import java.util.*


/**
 * 口座ドメインに対する顧客ユースケース処理。
 */
@Service
class AccountService(
        val rep: DefaultRepository
) {

    /** ログイン情報を取得します。  */
    @Transactional(DefaultRepository.BeanNameTx)
    @Cacheable("AccountService.getLoginByLoginId")
    fun getLoginByLoginId(loginId: String): Optional<Login> {
        return Login.getByLoginId(rep, loginId)
    }

    /** 有効な口座情報を取得します。  */
    @Transactional(DefaultRepository.BeanNameTx)
    @Cacheable("AccountService.getAccount")
    fun getAccount(id: String): Optional<Account> {
        return Account.getValid(rep, id)
    }

}
