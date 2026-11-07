package com.lucianmarin.news.state

import android.content.Context
import android.content.SharedPreferences
import com.lucianmarin.news.model.FeedTab
import com.lucianmarin.news.network.APIClient
import com.lucianmarin.news.ui.theme.ThemeMode

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
}

class InMemoryNewsPreferencesStore(
    private var selectedTab: FeedTab = FeedTab.HOT,
    private var visitedUrls: Set<String> = emptySet(),
    private var visitedSites: Set<String> = emptySet(),
    private var apiBaseUrl: String = APIClient.DEFAULT_BASE_URL,
    private var themeMode: ThemeMode = ThemeMode.SYSTEM
) : NewsPreferencesStore {
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
}

class SessionStorage(context: Context) : NewsPreferencesStore {

    companion object {
        private const val PREFS_NAME = "hotdroid_preferences"
        private const val KEY_SELECTED_TAB = "hotdroid.ui.selectedTab"
        private const val KEY_VISITED_URLS = "hotdroid.history.visitedUrls"
        private const val KEY_VISITED_SITES = "hotdroid.history.visitedSites"
        private const val KEY_API_BASE_URL = "hotdroid.config.apiBaseUrl"
        private const val KEY_THEME_MODE = "hotdroid.ui.themeMode"
    }

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

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
}
