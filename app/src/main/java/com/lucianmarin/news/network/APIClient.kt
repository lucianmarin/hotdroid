package com.lucianmarin.news.network

import com.lucianmarin.news.model.FeedTab
import com.lucianmarin.news.model.PaginatedNewsResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class APIException(
    override val message: String,
    val statusCode: Int = 0
) : Exception(message)

class APIClient(
    baseUrl: String = DEFAULT_BASE_URL,
    private val transport: ((URL) -> Pair<Int, String>)? = null
) {
    @Volatile
    var baseUrl: String = normalizeBaseUrl(baseUrl)
        private set
    companion object {
        const val DEFAULT_BASE_URL = "https://news.lucianmarin.com"

        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
            encodeDefaults = true
        }

        fun normalizeBaseUrl(raw: String): String {
            var trimmed = raw.trim()
            while (trimmed.endsWith("/")) {
                trimmed = trimmed.dropLast(1)
            }
            if (trimmed.endsWith("/api")) {
                trimmed = trimmed.dropLast(4)
            }
            if (trimmed.isEmpty()) {
                return DEFAULT_BASE_URL
            }
            return trimmed
        }
    }

    fun updateBaseUrl(newBaseUrl: String) {
        baseUrl = normalizeBaseUrl(newBaseUrl)
    }

    fun buildUrl(path: String, page: Int = 1): URL {
        val cleanBase = normalizeBaseUrl(baseUrl)
        val normalizedPath = if (path.startsWith("/")) path else "/$path"
        val query = if (page > 1) "?p=$page" else ""
        return URL("$cleanBase$normalizedPath$query")
    }

    suspend fun fetchFeed(tab: FeedTab, page: Int = 1): PaginatedNewsResponse =
        fetchEntries(path = tab.apiPath, page = page)

    suspend fun fetchSite(site: String, page: Int = 1): PaginatedNewsResponse {
        val cleaned = site.trim()
        if (cleaned.isEmpty()) {
            throw APIException("Invalid site name")
        }
        val encoded = URLEncoder.encode(cleaned, "UTF-8").replace("+", "%20")
        return fetchEntries(path = "/api/$encoded", page = page)
    }

    suspend fun fetchEntries(path: String, page: Int = 1): PaginatedNewsResponse =
        withContext(Dispatchers.IO) {
            val url = try {
                buildUrl(path, page)
            } catch (_: Exception) {
                throw APIException("Invalid API URL")
            }

            val (statusCode, rawBody) = try {
                transport?.invoke(url) ?: executeGet(url)
            } catch (e: APIException) {
                throw e
            } catch (e: Exception) {
                throw APIException(e.message ?: "Network error")
            }

            when (statusCode) {
                in 200..299 -> Unit
                404 -> throw APIException("Feed not found", 404)
                else -> throw APIException("Server returned HTTP $statusCode", statusCode)
            }

            try {
                json.decodeFromString<PaginatedNewsResponse>(rawBody)
            } catch (e: Exception) {
                throw APIException("Failed to parse news feed: ${e.message}", statusCode)
            }
        }

    private fun executeGet(url: URL): Pair<Int, String> {
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "Hotdroid/1.0 (Android)")
        }

        return try {
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val responseText = stream?.let {
                BufferedReader(InputStreamReader(it, Charsets.UTF_8)).use { reader ->
                    reader.readText()
                }
            } ?: ""
            code to responseText
        } finally {
            conn.disconnect()
        }
    }
}
