package sample.controller

import org.slf4j.LoggerFactory
import org.springframework.context.MessageSource
import org.springframework.context.MessageSourceResolvable
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.BindException
import org.springframework.validation.ObjectError
import org.springframework.web.HttpMediaTypeException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.ServletRequestBindingException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.HttpClientErrorException
import sample.ErrorKeys
import sample.ValidationException
import sample.Warn
import sample.Warns
import sample.context.actor.ActorSession
import java.io.IOException
import java.util.*
import javax.persistence.EntityNotFoundException
import javax.validation.ConstraintViolationException

/**
 * REST用の例外Map変換サポート。
 *
 * AOPアドバイスで全てのRestControllerに対して例外処理を当て込みます。
 */
@ControllerAdvice(annotations = [RestController::class])
class RestErrorAdvice(
        private val msg: MessageSource,
        private val session: ActorSession
) {

    val log = LoggerFactory.getLogger(javaClass)!!

    /** Servlet例外  */
    @ExceptionHandler(ServletRequestBindingException::class)
    fun handleServletRequestBinding(e: ServletRequestBindingException): ResponseEntity<Map<String, Array<String>>> {
        log.warn(e.message)
        return ErrorHolder(msg, locale(), "error.ServletRequestBinding").result(HttpStatus.BAD_REQUEST)
    }

    private fun locale(): Locale = session.actor().locale

    /** メッセージ内容の読み込み失敗例外  */
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadable(e: HttpMessageNotReadableException): ResponseEntity<Map<String, Array<String>>> {
        log.warn(e.message)
        return ErrorHolder(msg, locale(), "error.HttpMessageNotReadable").result(HttpStatus.BAD_REQUEST)
    }

    /** メディアタイプのミスマッチ例外  */
    @ExceptionHandler(HttpMediaTypeException::class)
    fun handleHttpMediaTypeException(
            e: HttpMediaTypeException): ResponseEntity<Map<String, Array<String>>> {
        log.warn(e.message)
        return ErrorHolder(msg, locale(), "error.HttpMediaTypeNotAcceptable").result(HttpStatus.BAD_REQUEST)
    }

    /** 楽観的排他(Hibernateのバージョンチェック)の例外  */
    @ExceptionHandler(OptimisticLockingFailureException::class)
    fun handleOptimisticLockingFailureException(
            e: OptimisticLockingFailureException): ResponseEntity<Map<String, Array<String>>> {
        log.warn(e.message, e)
        return ErrorHolder(msg, locale(), "error.OptimisticLockingFailure").result(HttpStatus.BAD_REQUEST)
    }

    /** 権限例外  */
    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDeniedException(e: AccessDeniedException): ResponseEntity<Map<String, Array<String>>> {
        log.warn(e.message)
        return ErrorHolder(msg, locale(), ErrorKeys.AccessDenied).result(HttpStatus.UNAUTHORIZED)
    }

    /** 指定した情報が存在しない例外  */
    @ExceptionHandler(EntityNotFoundException::class)
    fun handleEntityNotFoundException(e: EntityNotFoundException): ResponseEntity<Map<String, Array<String>>> {
        log.warn(e.message, e)
        return ErrorHolder(msg, locale(), ErrorKeys.EntityNotFound).result(HttpStatus.BAD_REQUEST)
    }

    /** BeanValidation(JSR303)の制約例外  */
    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(e: ConstraintViolationException): ResponseEntity<Map<String, Array<String>>> {
        log.warn(e.message)
        val warns = Warns.init()
        e.constraintViolations.forEach { warns.add(message = it.message, field = it.propertyPath.toString()) }
        return ErrorHolder(msg, locale(), warns.list).result(HttpStatus.BAD_REQUEST)
    }

    /** Controllerへのリクエスト紐付け例外(for JSON)  */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValid(e: MethodArgumentNotValidException): ResponseEntity<Map<String, Array<String>>> {
        log.warn(e.message)
        return ErrorHolder(msg, locale(), convert(e.bindingResult.allErrors).list)
                .result(HttpStatus.BAD_REQUEST)
    }

    /** Controllerへのリクエスト紐付け例外  */
    @ExceptionHandler(BindException::class)
    fun handleBind(e: BindException): ResponseEntity<Map<String, Array<String>>> {
        log.warn(e.message)
        return ErrorHolder(msg, locale(), convert(e.allErrors).list).result(HttpStatus.BAD_REQUEST)
    }

    protected fun convert(errors: List<ObjectError>): Warns {
        val warns = Warns.init()
        errors.forEach { oe ->
            var field = ""
            if (1 == oe.codes!!.size) {
                field = bindField(oe.codes!![0])
            } else if (1 < oe.codes!!.size) {
                // low: プリフィックスは冗長なので外してます
                field = bindField(oe.codes!![1])
            }
            val args = oe.arguments!!
                    .filter { it !is MessageSourceResolvable }
                    .map { it.toString() }
            var message = oe.defaultMessage
            if (0 <= oe.codes!![0].indexOf("typeMismatch")) {
                message = oe.codes!![2]
            }
            warns.add(message = message!!, field = field, messageArgs = args.toTypedArray())
        }
        return warns
    }

    protected fun bindField(field: String): String =
            Optional.ofNullable(field)
                    .map { it.substring(it.indexOf('.') + 1) }
                    .orElse("")

    /** RestTemplate 例外時のブリッジサポート  */
    @ExceptionHandler(HttpClientErrorException::class)
    fun handleHttpClientError(e: HttpClientErrorException): ResponseEntity<String> {
        val headers = HttpHeaders()
        headers[HttpHeaders.CONTENT_TYPE] = Arrays.asList(MediaType.APPLICATION_JSON_VALUE)
        return ResponseEntity(e.responseBodyAsString, headers, e.statusCode)
    }

    /** アプリケーション例外  */
    @ExceptionHandler(ValidationException::class)
    fun handleValidation(e: ValidationException): ResponseEntity<Map<String, Array<String>>> {
        log.warn(e.message)
        return ErrorHolder(msg, locale(), e).result(HttpStatus.BAD_REQUEST)
    }

    /** IO例外（Tomcatの Broken pipe はサーバー側の責務ではないので除外しています)  */
    @ExceptionHandler(IOException::class)
    fun handleIOException(e: IOException): ResponseEntity<Map<String, Array<String>>> =
            when (e.message != null && e.message!!.contains("Broken pipe")) {
                true -> {
                    log.info("クライアント事由で処理が打ち切られました。")
                    ResponseEntity(HttpStatus.OK)
                }
                else -> handleException(e)
            }

    /** 汎用例外  */
    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<Map<String, Array<String>>> {
        log.error("予期せぬ例外が発生しました。", e)
        return ErrorHolder(msg, locale(), ErrorKeys.Exception, "サーバー側で問題が発生した可能性があります。")
                .result(HttpStatus.INTERNAL_SERVER_ERROR)
    }

}

/**
 * 例外情報のスタックを表現します。
 *
 * スタックした例外情報は[.result]を呼び出す事でMapを持つResponseEntityへ変換可能です。
 * Mapのkeyはfiled指定値、valueはメッセージキーの変換値(messages-validation.properties)が入ります。
 *
 * [.errorGlobal]で登録した場合のキーは空文字となります。
 *
 * クライアント側は戻り値を [{"fieldA": "messageA"}, {"fieldB": "messageB"}]で受け取ります。
 */
class ErrorHolder(
        private val msg: MessageSource,
        private val locale: Locale,
        private val errors: MutableMap<String, MutableList<String>> = mutableMapOf()
) {

    constructor(msg: MessageSource, locale: Locale, e: ValidationException) : this(msg, locale, e.list) {}

    constructor(msg: MessageSource, locale: Locale, warns: List<Warn>) : this(msg, locale) {
        warns.forEach { warn ->
            if (warn.global)
                errorGlobal(warn.message)
            else
                error(warn.field!!, warn.message, *warn.messageArgs)
        }
    }

    constructor(msg: MessageSource, locale: Locale, globalMsgKey: String, vararg msgArgs: String) : this(msg, locale) {
        errorGlobal(globalMsgKey, *msgArgs)
    }

    /** グローバルな例外(フィールドキーが空)を追加します。  */
    private fun errorGlobalOrDefault(msgKey: String, defaultMsg: String, vararg msgArgs: String): ErrorHolder {
        if (!errors.containsKey("")) {
            errors[""] = mutableListOf()
        }
        errors[""]?.add(msg.getMessage(msgKey, msgArgs, defaultMsg, locale)!!)
        return this
    }

    /** グローバルな例外(フィールドキーが空)を追加します。  */
    private fun errorGlobal(msgKey: String, vararg msgArgs: String): ErrorHolder =
            errorGlobalOrDefault(msgKey, msgKey, *msgArgs)

    /** フィールド単位の例外を追加します。  */
    fun error(field: String, msgKey: String, vararg msgArgs: String): ErrorHolder {
        if (!errors.containsKey(field)) {
            errors[field] = mutableListOf()
        }
        errors[field]?.add(msg.getMessage(msgKey, msgArgs, msgKey, locale)!!)
        return this
    }

    /** 保有する例外情報をResponseEntityへ変換します。  */
    fun result(status: HttpStatus): ResponseEntity<Map<String, Array<String>>> =
            ResponseEntity(errors.entries.associateBy({ it.key }, { it.value.toTypedArray() }), status)

}
