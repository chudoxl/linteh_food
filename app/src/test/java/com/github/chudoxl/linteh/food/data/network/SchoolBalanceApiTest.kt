package com.github.chudoxl.linteh.food.data.network

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * prod.md §9 — двухэтапная авторизация и таймаут 30 секунд.
 *
 * SchoolBalanceApi принимает baseUrl и OkHttpClient через конструктор,
 * поэтому интеграционные сценарии (двухэтапная авторизация, кэш сессии,
 * структура POST-запроса) тестируются через MockWebServer без рефлексии.
 * Инварианты прод-конфига (TTL сессии, таймауты) проверяются через
 * публичные константы в companion object.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SchoolBalanceApiTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    /** §9 — TTL кэша сессии равен 4 минутам. */
    @Test
    fun session_ttl_is_4_minutes() {
        // prod.md §9: Сессия переиспользуется между запросами в течение 4 минут.
        assertThat(SchoolBalanceApi.DEFAULT_SESSION_TTL_MS).isEqualTo(4 * 60 * 1000L)
    }

    /** §9 — таймауты connect/read настроены на 30 секунд. */
    @Test
    fun timeout_must_match_spec_30_seconds() {
        // prod.md §9: Таймаут на каждый сетевой запрос — 30 секунд.
        assertThat(SchoolBalanceApi.CONNECT_TIMEOUT_SECONDS).isEqualTo(30L)
        assertThat(SchoolBalanceApi.READ_TIMEOUT_SECONDS).isEqualTo(30L)
    }

    /** §9 — отсутствие PHPSESSID в ответе первого запроса бросает «Не удалось получить сессию». */
    @Test
    fun missing_phpsessid_throws_session_error() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(""))

        val api = buildApiPointingTo(server)
        val ex = assertThrows(IllegalStateException::class.java) {
            runBlocking { api.getBalance(login = "001", password = "pwd") }
        }
        assertThat(ex.message).isEqualTo("Не удалось получить сессию")
    }

    /** §9 — HTTP-ошибка сервера (500) оборачивается в IllegalStateException с кодом в сообщении. */
    @Test
    fun server_error_is_wrapped_in_illegal_state() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .addHeader("Set-Cookie", "PHPSESSID=ABC; path=/")
                .setBody("")
        )
        server.enqueue(MockResponse().setResponseCode(500).setBody("boom"))

        val api = buildApiPointingTo(server)
        val ex = assertThrows(IllegalStateException::class.java) {
            runBlocking { api.getBalance(login = "001", password = "pwd") }
        }
        assertThat(ex.message).contains("500")
    }

    /** §9 — в пределах TTL второй вызов getBalance переиспользует кэшированный PHPSESSID. */
    @Test
    fun second_call_reuses_cached_session_within_ttl() = runTest {
        val html = "<form></form>"
        // 1-й GET сессии
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .addHeader("Set-Cookie", "PHPSESSID=CACHED; path=/")
                .setBody(html)
        )
        // 1-й POST баланса — упадёт на парсинге (невалидный JSON), нам важен POST-хедер
        server.enqueue(MockResponse().setResponseCode(200).setBody("not json"))
        // 2-й POST баланса — тоже упадёт
        server.enqueue(MockResponse().setResponseCode(200).setBody("not json"))

        val api = buildApiPointingTo(server)
        runCatching { runBlocking { api.getBalance("001", "pwd") } }
        runCatching { runBlocking { api.getBalance("001", "pwd") } }

        // Должно быть 3 запроса суммарно: 1 GET + 2 POST. Если бы кэш не работал, было бы 2 GET + 2 POST.
        assertThat(server.requestCount).isEqualTo(3)

        server.takeRequest() // GET /informaciya-o-pitanii
        val firstPost = server.takeRequest()
        val secondPost = server.takeRequest()
        assertThat(firstPost.getHeader("Cookie")).isEqualTo("PHPSESSID=CACHED")
        assertThat(secondPost.getHeader("Cookie")).isEqualTo("PHPSESSID=CACHED")
    }

    /** §9 — POST баланса содержит login, pwd, Cookie с PHPSESSID и заголовок X-Requested-With. */
    @Test
    fun post_request_contains_login_pwd_and_cookie() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .addHeader("Set-Cookie", "PHPSESSID=ABC; path=/")
                .setBody("")
        )
        server.enqueue(MockResponse().setResponseCode(200).setBody("not json"))

        val api = buildApiPointingTo(server)
        runCatching { runBlocking { api.getBalance(login = "001002003", password = "6PASSWORD9") } }

        server.takeRequest() // GET
        val post = server.takeRequest()
        val body = post.body.readUtf8()
        assertThat(body).contains("login=001002003")
        assertThat(body).contains("pwd=6PASSWORD9")
        assertThat(post.getHeader("Cookie")).isEqualTo("PHPSESSID=ABC")
        assertThat(post.getHeader("X-Requested-With")).isEqualTo("XMLHttpRequest")
    }

    // --- helpers ---------------------------------------------------------

    private fun buildApiPointingTo(server: MockWebServer): SchoolBalanceApi {
        val baseUrl = server.url("/").toString().trimEnd('/')
        val client = OkHttpClient.Builder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.SECONDS)
            .followRedirects(false)
            .build()
        return SchoolBalanceApi(baseUrl = baseUrl, client = client)
    }
}
