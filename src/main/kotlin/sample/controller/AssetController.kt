package sample.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import sample.ActionStatusType
import sample.context.Dto
import sample.model.asset.CashInOut
import sample.model.asset.RegCashOut
import sample.usecase.AssetService
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import javax.validation.Valid


/**
 * 資産に関わる顧客のUI要求を処理します。
 */
@RestController
@RequestMapping("/api/asset")
class AssetController(private val service: AssetService) {

    /** 未処理の振込依頼情報を検索します。  */
    @GetMapping("/cio/unprocessedOut/")
    fun findUnprocessedCashOut(): List<CashOutUI> =
        service.findUnprocessedCashOut().map { CashOutUI.of(it) }

    /**
     * 振込出金依頼をします。
     * low: RestControllerの標準の振る舞いとしてvoidやプリミティブ型はJSON化されないので注意してください。
     * (解析時の優先順位の関係だと思いますが)
     */
    @PostMapping("/cio/withdraw")
    fun withdraw(@Valid p: RegCashOut): ResponseEntity<Long> =
            ResponseEntity.ok(service.withdraw(p))

}

/** 振込出金依頼情報の表示用Dto  */
data class CashOutUI(
        val id: Long,
        val currency: String,
        val absAmount: BigDecimal,
        val requestDay: LocalDate,
        val requestDate: LocalDateTime,
        val eventDay: LocalDate,
        val valueDay: LocalDate,
        val statusType: ActionStatusType,
        val updateDate: LocalDateTime,
        val cashflowId: Long? = null
) : Dto {

    companion object {
        private const val serialVersionUID = 1L

        fun of(cio: CashInOut): CashOutUI =
                CashOutUI(cio.id!!, cio.currency, cio.absAmount, cio.requestDay,
                        cio.requestDate, cio.eventDay, cio.valueDay, cio.statusType,
                        cio.updateDate!!, cio.cashflowId)
    }
}
