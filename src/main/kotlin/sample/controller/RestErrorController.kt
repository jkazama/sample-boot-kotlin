package sample.controller

import org.springframework.boot.web.error.ErrorAttributeOptions
import org.springframework.boot.web.servlet.error.ErrorAttributes
import org.springframework.boot.web.servlet.error.ErrorController
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.request.ServletWebRequest

/**
 * REST用の例外ハンドリングを行うController。
 *
 * application.ymlの"error.path"属性との組合せで有効化します。 あわせて"error.whitelabel.enabled:
 * false"でwhitelabelを無効化しておく必要があります。 see ErrorMvcAutoConfiguration
 */
@RestController
class RestErrorController(private val errorAttributes: ErrorAttributes) : ErrorController {

    @RequestMapping(PathError)
    fun error(request: ServletWebRequest): Map<String, Any> =
            this.errorAttributes.getErrorAttributes(request, ErrorAttributeOptions.defaults())

    companion object {
        const val PathError = "/api/error"
    }
}
