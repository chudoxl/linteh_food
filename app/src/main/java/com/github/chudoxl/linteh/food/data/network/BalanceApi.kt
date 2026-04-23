package com.github.chudoxl.linteh.food.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.concurrent.TimeUnit

data class BalanceResponse(
    val studentName: String,
    val balance: BigDecimal,
)

interface BalanceApi {
    suspend fun getBalance(login: String, password: String): BalanceResponse
}

class SchoolBalanceApi(
    private val baseUrl: String = DEFAULT_BASE_URL,
    private val client: OkHttpClient = defaultClient(),
    private val sessionTtlMs: Long = DEFAULT_SESSION_TTL_MS,
) : BalanceApi {

    private var cachedSessionId: String? = null
    private var sessionTimestamp: Long = 0

    override suspend fun getBalance(login: String, password: String): BalanceResponse =
        withContext(Dispatchers.IO) {
            val sessionId = getOrRefreshSession()
            val json = postBalanceRequest(sessionId, login, password)
            parseResponse(json)
        }

    private fun getOrRefreshSession(): String {
        val now = System.currentTimeMillis()
        val cached = cachedSessionId
        if (cached != null && now - sessionTimestamp < sessionTtlMs) {
            return cached
        }

        val request = Request.Builder()
            .url("$baseUrl/informaciya-o-pitanii")
            .get()
            .build()

        val response = client.newCall(request).execute()
        response.close()

        val sessionId = response.headers("Set-Cookie")
            .firstOrNull { it.startsWith("PHPSESSID=") }
            ?.substringAfter("PHPSESSID=")
            ?.substringBefore(";")
            ?: throw IllegalStateException("Не удалось получить сессию")

        cachedSessionId = sessionId
        sessionTimestamp = now
        return sessionId
    }

    private fun postBalanceRequest(sessionId: String, login: String, password: String): String {
        val body = FormBody.Builder()
            .add("resource", "2456")
            .add("ya_target", "mealsform")
            .add("send_mealsform", "1")
            .add("country", "")
            .add("login", login)
            .add("pwd", password)
            .add("ajaxchunk_name", "mealsform")
            .add("timestamp", System.currentTimeMillis().toString())
            .build()

        val request = Request.Builder()
            .url("$baseUrl/assets/components/ajaxchunk/connector.php")
            .post(body)
            .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            .header("Accept", "application/json, text/javascript, */*; q=0.01")
            .header("X-Requested-With", "XMLHttpRequest")
            .header("Origin", baseUrl)
            .header("Referer", "$baseUrl/informaciya-o-pitanii")
            .header("Cookie", "PHPSESSID=$sessionId")
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string()
            ?: throw IllegalStateException("Пустой ответ сервера")

        if (!response.isSuccessful) {
            throw IllegalStateException("Ошибка сервера: ${response.code}")
        }

        return responseBody
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://school28-kirov.ru"
        const val DEFAULT_SESSION_TTL_MS = 4 * 60 * 1000L
        const val CONNECT_TIMEOUT_SECONDS = 30L
        const val READ_TIMEOUT_SECONDS = 30L

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .followRedirects(false)
            .build()

        private val NAME_REGEX =
            Regex("""<B>\\u0421\\u043e\\u0441\\u0442\\u043e\\u044f\\u043d\\u0438\\u0435[^<]*</B>.*?<B>([^<]+)</B>""", RegexOption.DOT_MATCHES_ALL)

        private val NAME_DECODED_REGEX =
            Regex("""<B>\s*Состояние лицевого сч[её]та\s*</B>.*?<B>([^<]+)</B>""", RegexOption.DOT_MATCHES_ALL)

        private val LAST_BALANCE_ROW_REGEX =
            Regex("""Остаток на[^<]*</TD>\s*<TD[^>]*COLSPAN=2[^>]*>([^<]+)</TD>\s*<TD[^>]*COLSPAN=2[^>]*>([^<]+)</TD>""", RegexOption.DOT_MATCHES_ALL)

        fun parseResponse(json: String): BalanceResponse {
            val jsonObj = JSONObject(json)
            if (!jsonObj.optBoolean("success", false)) {
                throw IllegalStateException("Сервер вернул ошибку")
            }
            val content = jsonObj.getString("content")

            val decoded = decodeUnicodeEscapes(content)

            val studentName = NAME_DECODED_REGEX.find(decoded)?.groupValues?.get(1)?.trim()
                ?: "Неизвестный"

            val balanceMatch = LAST_BALANCE_ROW_REGEX.findAll(decoded).lastOrNull()
                ?: throw IllegalStateException("Не удалось найти данные о балансе в ответе")

            val expenseStr = balanceMatch.groupValues[1]
            val incomeStr = balanceMatch.groupValues[2]

            val expense = parseNumber(expenseStr)
            val income = parseNumber(incomeStr)
            val balance = income.add(expense)

            return BalanceResponse(
                studentName = studentName,
                balance = balance.setScale(2, RoundingMode.HALF_UP),
            )
        }

        private fun parseNumber(raw: String): BigDecimal {
            val cleaned = raw
                .replace("\\u00a0", "")
                .replace("\u00a0", "")
                .replace("&nbsp;", "")
                .replace(" ", "")
                .replace(",", ".")
                .trim()
            if (cleaned == "-" || cleaned.isBlank()) return BigDecimal.ZERO
            return BigDecimal(cleaned)
        }

        private fun decodeUnicodeEscapes(input: String): String {
            val regex = Regex("""\\u([0-9a-fA-F]{4})""")
            return regex.replace(input) { match ->
                val codePoint = match.groupValues[1].toInt(16)
                codePoint.toChar().toString()
            }
        }
    }
}

