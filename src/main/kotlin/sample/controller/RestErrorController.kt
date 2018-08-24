package sample.controller

import org.springframework.web.context.request.ServletWebRequest
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.web.servlet.error.ErrorAttributes
import org.springframework.boot.web.servlet.error.ErrorController
import org.springframework.web.bind.annotation.RestController


/**
 * REST用の例外ハンドリングを行うController。
 *
 * application.ymlの"error.path"属性との組合せで有効化します。
 * あわせて"error.whitelabel.enabled: false"でwhitelabelを無効化しておく必要があります。
 * see ErrorMvcAutoConfiguration
 */
@RestController
class RestErrorController(
        val errorAttributes: ErrorAttributes
): ErrorController {

    override fun getErrorPath(): String {
        return PathError
    }

    @RequestMapping(PathError)
    fun error(request: ServletWebRequest): Map<String, Any> {
        return this.errorAttributes.getErrorAttributes(request, false)
    }

    companion object {
        const val PathError = "/api/error"
    }

}
