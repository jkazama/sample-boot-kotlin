package sample.client

import java.io.IOException
import org.springframework.http.HttpMethod
import com.sun.corba.se.impl.protocol.giopmsgheaders.MessageBase.createRequest
import org.apache.commons.io.IOUtils
import org.junit.FixMethodOrder
import org.junit.Ignore
import org.junit.Test
import org.springframework.http.HttpStatus
import org.springframework.http.client.ClientHttpRequest
import org.springframework.http.client.ClientHttpResponse
import org.springframework.http.client.SimpleClientHttpRequestFactory
import sample.util.DateUtils
import sample.util.TimePoint
import java.net.URI
import java.util.*


/**
 * 単純なHTTP経由の実行検証。
 * <p>SpringがサポートするWebTestSupportでの検証で良いのですが、コンテナ立ち上げた後に叩く単純確認用に作りました。
 * <p>「extention.security.auth.enabled: true」の時は実際にログインして処理を行います。
 * falseの時はDummyLoginInterceptorによる擬似ログインが行われます。
 */
@FixMethodOrder
@Ignore // 実行時はこの行をコメントアウトしてください。
class SampleClient {

    // 「extention.security.auth.admin: false」の時のみ利用可能です。
    @Test
    fun 顧客向けユースケース検証() {
        val agent = SimpleTestAgent()
        agent.login("sample", "sample")
        agent.post("振込出金依頼", "/asset/cio/withdraw?accountId=sample&currency=JPY&absAmount=200")
        agent["振込出金依頼未処理検索", "/asset/cio/unprocessedOut/"]
    }

    // 「extention.security.auth.admin: true」の時のみ利用可能です。
    @Test
    fun 社内向けユースケース検証() {
        val day = DateUtils.dayFormat(TimePoint.now().day)
        val agent = SimpleTestAgent()
        agent.login("admin", "admin")
        agent["振込入出金依頼検索", "/admin/asset/cio/?updFromDay=$day&updToDay=$day"]
    }

    @Test
    fun バッチ向けユースケース検証() {
        val fromDay = DateUtils.dayFormat(TimePoint.now().day.minusDays(1))
        val toDay = DateUtils.dayFormat(TimePoint.now().day.plusDays(3))
        val agent = SimpleTestAgent()
        agent.post("営業日を進める(単純日回しのみ)", "/system/job/daily/processDay")
        agent.post("当営業日の出金依頼を締める", "/system/job/daily/closingCashOut")
        agent.post("入出金キャッシュフローを実現する(受渡日に残高へ反映)", "/system/job/daily/realizeCashflow")
        agent["イベントログを検索する", "/admin/system/audit/event/?fromDay=$fromDay&toDay=$toDay"]
    }

    /** 単純なSession概念を持つHTTPエージェント  */
    private inner class SimpleTestAgent {
        private val factory = SimpleClientHttpRequestFactory()
        private var sessionId = Optional.empty<String>()

        @Throws(Exception::class)
        fun path(path: String): URI {
            return URI(ROOT_PATH + path)
        }

        @Throws(Exception::class)
        fun login(loginId: String, password: String): SimpleTestAgent {
            val res = post("ログイン", "/login?loginId=$loginId&password=$password")
            if (res.getStatusCode() === HttpStatus.OK) {
                val cookieStr = res.getHeaders().get("Set-Cookie")!![0]
                sessionId = Optional.of(cookieStr.substring(0, cookieStr.indexOf(';')))
            }
            return this
        }

        @Throws(Exception::class)
        operator fun get(title: String, path: String): ClientHttpResponse {
            title(title)
            return dump(request(path, HttpMethod.GET).execute())
        }

        @Throws(Exception::class)
        private fun request(path: String, method: HttpMethod): ClientHttpRequest {
            val req = factory.createRequest(path(path), method)
            sessionId.ifPresent { req.headers.add("Cookie", it) }
            return req
        }

        @Throws(Exception::class)
        fun post(title: String, path: String): ClientHttpResponse {
            title(title)
            return dump(request(path, HttpMethod.POST).execute())
        }

        fun title(title: String) {
            println("------- $title------- ")
        }

        @Throws(Exception::class)
        fun dump(res: ClientHttpResponse): ClientHttpResponse {
            println(String.format("status: %d, text: %s", res.getRawStatusCode(), res.getStatusText()))
            try {
                System.out.println(IOUtils.toString(res.getBody(), "UTF-8"))
            } catch (e: IOException) {
                /* nothing. */
            }

            return res
        }

    }

    companion object {
        private const val ROOT_PATH = "http://localhost:8080/api"
    }

}
