package com.lucianmarin.news.state

import android.content.Context
import android.content.SharedPreferences
import com.lucianmarin.news.model.FeedTab
import com.lucianmarin.news.model.PaginatedNewsResponse
import com.lucianmarin.news.network.APIClient
import com.lucianmarin.news.ui.theme.ThemeMode
import java.util.concurrent.ConcurrentHashMap

interface NewsPreferencesStore {
    fun getSelectedTab(): FeedTab
    fun setSelectedTab(tab: FeedTab)
    fun getVisitedUrls(): Set<String>
    fun setVisitedUrls(urls: Set<String>)
    fun getVisitedSites(): Set<String>
    fun setVisitedSites(sites: Set<String>)
    fun getApiBaseUrl(): String
    fun setApiBaseUrl(url: String)
    fun getThemeMode(): ThemeMode
    fun setThemeMode(mode: ThemeMode)
    fun getEndpointCache(endpoint: String): PaginatedNewsResponse? = null
    fun setEndpointCache(endpoint: String, response: PaginatedNewsResponse) {}
    fun getAllEndpointCaches(): Map<String, PaginatedNewsResponse> = emptyMap()
    fun clearEndpointCaches() {}
}

class InMemoryNewsPreferencesStore(
    private var selectedTab: FeedTab = FeedTab.HOT,
    private var visitedUrls: Set<String> = emptySet(),
    private var visitedSites: Set<String> = emptySet(),
    private var apiBaseUrl: String = APIClient.DEFAULT_BASE_URL,
    private var themeMode: ThemeMode = ThemeMode.SYSTEM,
    initialEndpointCaches: Map<String, PaginatedNewsResponse> = emptyMap()
) : NewsPreferencesStore {
    private val endpointCaches = ConcurrentHashMap<String, PaginatedNewsResponse>()

    init {
        for ((endpoint, response) in initialEndpointCaches) {
            setEndpointCache(endpoint, response)
        }
    }

    override fun getSelectedTab(): FeedTab = selectedTab
    override fun setSelectedTab(tab: FeedTab) {
        selectedTab = tab
    }

    override fun getVisitedUrls(): Set<String> = visitedUrls
    override fun setVisitedUrls(urls: Set<String>) {
        visitedUrls = urls.toSet()
    }

    override fun getVisitedSites(): Set<String> = visitedSites
    override fun setVisitedSites(sites: Set<String>) {
        visitedSites = sites.toSet()
    }

    override fun getApiBaseUrl(): String = apiBaseUrl
    override fun setApiBaseUrl(url: String) {
        apiBaseUrl = APIClient.normalizeBaseUrl(url)
    }

    override fun getThemeMode(): ThemeMode = themeMode
    override fun setThemeMode(mode: ThemeMode) {
        themeMode = mode
    }

    override fun getEndpointCache(endpoint: String): PaginatedNewsResponse? {
        val key = APIClient.cacheKeyForEndpoint(endpoint) ?: return null
        return endpointCaches[key]
    }

    override fun setEndpointCache(endpoint: String, response: PaginatedNewsResponse) {
        if (response.page != 1) return
        val key = APIClient.cacheKeyForEndpoint(endpoint, page = response.page) ?: return
        endpointCaches[key] = response
    }

    override fun getAllEndpointCaches(): Map<String, PaginatedNewsResponse> =
        endpointCaches.toMap()

    override fun clearEndpointCaches() {
        endpointCaches.clear()
    }
}

class SessionStorage(context: Context) : NewsPreferencesStore {

    companion object {
        private const val PREFS_NAME = "hotdroid_preferences"
        private const val KEY_SELECTED_TAB = "hotdroid.ui.selectedTab"
        private const val KEY_VISITED_URLS = "hotdroid.history.visitedUrls"
        private const val KEY_VISITED_SITES = "hotdroid.history.visitedSites"
        private const val KEY_API_BASE_URL = "hotdroid.config.apiBaseUrl"
        private const val KEY_THEME_MODE = "hotdroid.ui.themeMode"
        private const val KEY_ENDPOINT_CACHE_PREFIX = "hotdroid.cache.endpoint."
    }

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val memoryEndpointCaches = ConcurrentHashMap<String, PaginatedNewsResponse>()

    override fun getSelectedTab(): FeedTab {
        val raw = prefs.getString(KEY_SELECTED_TAB, FeedTab.HOT.id)
        return FeedTab.fromId(raw)
    }

    override fun setSelectedTab(tab: FeedTab) {
        prefs.edit().putString(KEY_SELECTED_TAB, tab.id).apply()
    }

    override fun getVisitedUrls(): Set<String> =
        prefs.getStringSet(KEY_VISITED_URLS, emptySet())?.toSet() ?: emptySet()

    override fun setVisitedUrls(urls: Set<String>) {
        prefs.edit().putStringSet(KEY_VISITED_URLS, urls.toSet()).apply()
    }

    override fun getVisitedSites(): Set<String> =
        prefs.getStringSet(KEY_VISITED_SITES, emptySet())?.toSet() ?: emptySet()

    override fun setVisitedSites(sites: Set<String>) {
        prefs.edit().putStringSet(KEY_VISITED_SITES, sites.toSet()).apply()
    }

    override fun getApiBaseUrl(): String {
        val raw = prefs.getString(KEY_API_BASE_URL, APIClient.DEFAULT_BASE_URL)
        return APIClient.normalizeBaseUrl(raw ?: APIClient.DEFAULT_BASE_URL)
    }

    override fun setApiBaseUrl(url: String) {
        prefs.edit().putString(KEY_API_BASE_URL, APIClient.normalizeBaseUrl(url)).apply()
    }

    override fun getThemeMode(): ThemeMode {
        val raw = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.id)
        return ThemeMode.fromId(raw ?: ThemeMode.SYSTEM.id)
    }

    override fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.id).apply()
    }

    override fun getEndpointCache(endpoint: String): PaginatedNewsResponse? {
        val key = APIClient.cacheKeyForEndpoint(endpoint) ?: return null
        memoryEndpointCaches[key]?.let { return it }
        val rawJson = prefs.getString("$KEY_ENDPOINT_CACHE_PREFIX$key", null) ?: return null
        return try {
            APIClient.json.decodeFromString(PaginatedNewsResponse.serializer(), rawJson).also {
                if (it.page == 1) {
                    memoryEndpointCaches[key] = it
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    override fun setEndpointCache(endpoint: String, response: PaginatedNewsResponse) {
        if (response.page != 1) return
        val key = APIClient.cacheKeyForEndpoint(endpoint, page = response.page) ?: return
        memoryEndpointCaches[key] = response
        try {
            val encoded = APIClient.json.encodeToString(PaginatedNewsResponse.serializer(), response)
            prefs.edit().putString("$KEY_ENDPOINT_CACHE_PREFIX$key", encoded).apply()
        } catch (_: Exception) {
        }
    }

    override fun getAllEndpointCaches(): Map<String, PaginatedNewsResponse> {
        val result = mutableMapOf<String, PaginatedNewsResponse>()
        for ((prefKey, value) in prefs.all) {
            if (prefKey.startsWith(KEY_ENDPOINT_CACHE_PREFIX) && value is String) {
                val endpoint = prefKey.removePrefix(KEY_ENDPOINT_CACHE_PREFIX)
                val canonicalKey = APIClient.cacheKeyForEndpoint(endpoint) ?: continue
                try {
                    val decoded = APIClient.json.decodeFromString(PaginatedNewsResponse.serializer(), value)
                    if (decoded.page == 1) {
                        result[canonicalKey] = decoded
                    }
                } catch (_: Exception) {
                }
            }
        }
        result.putAll(memoryEndpointCaches)
        return result
    }

    override fun clearEndpointCaches() {
        memoryEndpointCaches.clear()
        val editor = prefs.edit()
        for (prefKey in prefs.all.keys) {
            if (prefKey.startsWith(KEY_ENDPOINT_CACHE_PREFIX)) {
                editor.remove(prefKey)
            }
        }
        editor.apply()
    }
}
