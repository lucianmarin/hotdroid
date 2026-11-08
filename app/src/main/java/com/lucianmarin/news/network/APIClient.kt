package com.lucianmarin.news.network

import com.lucianmarin.news.model.FeedTab
import com.lucianmarin.news.model.PaginatedNewsResponse
import com.lucianmarin.news.state.NewsPreferencesStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

class APIException(
    override val message: String,
    val statusCode: Int = 0
) : Exception(message)

class APIClient(
    baseUrl: String = DEFAULT_BASE_URL,
    private val cacheStore: NewsPreferencesStore? = null,
    private val transport: ((URL) -> Pair<Int, String>)? = null
) {
    @Volatile
    var baseUrl: String = normalizeBaseUrl(baseUrl)
        private set

    @Volatile
    private var attachedCacheStore: NewsPreferencesStore? = cacheStore

    private val endpointCache = ConcurrentHashMap<String, PaginatedNewsResponse>()

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

        fun siteEndpointPath(site: String): String {
            val cleaned = site.trim()
            if (cleaned.isEmpty()) {
                throw APIException("Invalid site name")
            }
            val encoded = URLEncoder.encode(cleaned, "UTF-8").replace("+", "%20")
            return "/api/$encoded"
        }

        /**
         * Parses an endpoint path or URL and resolves its effective page number.
         * `?p=1` is treated as equal to a plain endpoint call without `p` parameters.
         */
        fun parseEndpointAndPage(rawPathOrUrl: String, page: Int = 1): Pair<String, Int> {
            val (cleanPath, query) = splitPathAndQuery(rawPathOrUrl)
            val queryPage = parsePageQueryParam(query)
            val effectivePage = when {
                page != 1 -> page
                queryPage != null -> queryPage
                else -> 1
            }
            return cleanPath to effectivePage
        }

        /**
         * Returns the canonical endpoint cache key (e.g. `/api/hot`, `/api/cold`, `/api/new`, `/api/{site}`)
         * when the request targets the first page (plain endpoint or `?p=1` with `page == 1`),
         * or `null` if the call is for any other page (`?p=2`, `page > 1`, etc.).
         */
        fun cacheKeyForEndpoint(rawPathOrUrl: String, page: Int = 1): String? {
            if (page != 1) return null
            val (cleanPath, query) = splitPathAndQuery(rawPathOrUrl)
            if (cleanPath.isEmpty() || cleanPath == "/") return null

            if (query.isNotEmpty()) {
                val params = query.split("&").filter { it.isNotBlank() }
                if (params.isEmpty()) return cleanPath
                val allFirstPageParam = params.all { param ->
                    val parts = param.split("=", limit = 2)
                    parts.size == 2 && parts[0] == "p" && parts[1] == "1"
                }
                if (!allFirstPageParam) {
                    return null
                }
            }

            return cleanPath
        }

        private fun splitPathAndQuery(rawPathOrUrl: String): Pair<String, String> {
            var input = rawPathOrUrl.trim()
            if (input.isEmpty()) return "" to ""

            if (input.contains("://")) {
                input = try {
                    val uri = URI(input)
                    val uriPath = uri.rawPath ?: ""
                    val uriQuery = uri.rawQuery
                    if (uriQuery.isNullOrEmpty()) uriPath else "$uriPath?$uriQuery"
                } catch (_: Exception) {
                    val afterScheme = input.substringAfter("://")
                    if (afterScheme.contains("/")) {
                        "/" + afterScheme.substringAfter("/")
                    } else {
                        ""
                    }
                }
            }

            val rawPath = input.substringBefore("?").trim()
            val rawQuery = if (input.contains("?")) input.substringAfter("?").trim() else ""

            if (rawPath.isEmpty()) return "" to rawQuery

            var cleanPath = if (rawPath.startsWith("/")) rawPath else "/$rawPath"
            while (cleanPath.length > 1 && cleanPath.endsWith("/")) {
                cleanPath = cleanPath.dropLast(1)
            }
            return cleanPath to rawQuery
        }

        private fun parsePageQueryParam(query: String): Int? {
            if (query.isEmpty()) return null
            val pValue = query
                .split("&")
                .firstOrNull { it.startsWith("p=") }
                ?.substringAfter("p=")
                ?.trim()
                ?: return null
            return pValue.toIntOrNull()
        }
    }

    fun attachCacheStore(store: NewsPreferencesStore) {
        attachedCacheStore = store
        for ((endpoint, response) in endpointCache) {
            store.setEndpointCache(endpoint, response)
        }
    }

    fun updateBaseUrl(newBaseUrl: String) {
        baseUrl = normalizeBaseUrl(newBaseUrl)
    }

    fun buildUrl(path: String, page: Int = 1): URL {
        val cleanBase = normalizeBaseUrl(baseUrl)
        val (normalizedPath, effectivePage) = parseEndpointAndPage(path, page)
        if (normalizedPath.isEmpty() || effectivePage < 1) {
            throw APIException("Invalid API URL")
        }
        val query = if (effectivePage > 1) "?p=$effectivePage" else ""
        return URL("$cleanBase$normalizedPath$query")
    }

    fun getCachedResponse(path: String, page: Int = 1): PaginatedNewsResponse? {
        val key = cacheKeyForEndpoint(path, page) ?: return null
        endpointCache[key]?.let { return it }
        val stored = attachedCacheStore?.getEndpointCache(key)
        if (stored != null) {
            endpointCache[key] = stored
        }
        return stored
    }

    fun getCachedFeed(tab: FeedTab, page: Int = 1): PaginatedNewsResponse? =
        getCachedResponse(path = tab.apiPath, page = page)

    fun getCachedSite(site: String, page: Int = 1): PaginatedNewsResponse? {
        val path = try {
            siteEndpointPath(site)
        } catch (_: APIException) {
            return null
        }
        return getCachedResponse(path = path, page = page)
    }

    fun cacheResponse(path: String, response: PaginatedNewsResponse, page: Int = response.page) {
        if (response.page != 1) return
        val key = cacheKeyForEndpoint(path, page) ?: return
        endpointCache[key] = response
        attachedCacheStore?.setEndpointCache(key, response)
    }

    fun getCachedEndpoints(): Map<String, PaginatedNewsResponse> {
        val persisted = attachedCacheStore?.getAllEndpointCaches().orEmpty()
        return persisted + endpointCache
    }

    fun clearCache() {
        endpointCache.clear()
        attachedCacheStore?.clearEndpointCaches()
    }

    suspend fun fetchFeed(tab: FeedTab, page: Int = 1): PaginatedNewsResponse =
        fetchEntries(path = tab.apiPath, page = page)

    suspend fun fetchSite(site: String, page: Int = 1): PaginatedNewsResponse {
        val endpointPath = siteEndpointPath(site)
        return fetchEntries(path = endpointPath, page = page)
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

            val decoded = try {
                json.decodeFromString<PaginatedNewsResponse>(rawBody)
            } catch (e: Exception) {
                throw APIException("Failed to parse news feed: ${e.message}", statusCode)
            }

            val (_, effectivePage) = parseEndpointAndPage(path, page)
            if (effectivePage == 1 && decoded.page == 1) {
                cacheResponse(path = path, response = decoded, page = effectivePage)
            }

            decoded
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
