package sample.controller.admin

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import sample.model.asset.CashInOut
import sample.model.asset.FindCashInOut
import sample.usecase.admin.AssetAdminService
import javax.validation.Valid

/**
 * 資産に関わる社内のUI要求を処理します。
 */
@RestController
@RequestMapping("/api/admin/asset")
class AssetAdminController(val service: AssetAdminService) {

    /** 未処理の振込依頼情報を検索します。  */
    @GetMapping("/cio/")
    fun findCashInOut(@Valid p: FindCashInOut): List<CashInOut> =
            service.findCashInOut(p)

}