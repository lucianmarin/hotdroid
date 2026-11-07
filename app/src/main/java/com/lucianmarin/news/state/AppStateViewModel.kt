package com.lucianmarin.news.state

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lucianmarin.news.model.FeedTab
import com.lucianmarin.news.model.NewsEntry
import com.lucianmarin.news.network.APIClient
import com.lucianmarin.news.ui.theme.ThemeMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.max

data class AppUiState(
    val selectedTab: FeedTab = FeedTab.HOT,
    val selectedSite: String? = null,
    val entries: List<NewsEntry> = emptyList(),
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val loadMoreError: Boolean = false,
    val errorMessage: String? = null,
    val visitedUrls: Set<String> = emptySet(),
    val visitedSites: Set<String> = emptySet(),
    val apiBaseUrl: String = APIClient.DEFAULT_BASE_URL,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val scrollResetVersion: Int = 0
) {
    val canLoadMore: Boolean
        get() = currentPage < totalPages && entries.isNotEmpty()

    val isSiteSelected: Boolean
        get() = !selectedSite.isNullOrBlank()

    val activeFeedPath: String
        get() = if (!selectedSite.isNullOrBlank()) {
            "/api/$selectedSite"
        } else {
            selectedTab.apiPath
        }
}

class NewsStateController(
    private val storage: NewsPreferencesStore,
    val apiClient: APIClient,
    private val scope: CoroutineScope
) {
    companion object {
        /**
         * Odd news rows (1st, 3rd, 5th... in 1-based display order, i.e. 0-based `index % 2 == 0`)
         * are highlighted (`#F0F0F0` Light / `#141414` Dark), and even rows (`index % 2 == 1`) are not.
         */
        fun isOddRowHighlighted(zeroBasedIndex: Int): Boolean {
            val oneBasedRowNumber = zeroBasedIndex + 1
            return oneBasedRowNumber % 2 == 1
        }

        /**
         * Returns whether the space after the last item in the list should use `oddRowBackground`
         * (`true`) or `evenRowBackground` (`false`), matching the last row's background.
         */
        fun isLastRowOddHighlighted(entryCount: Int): Boolean {
            if (entryCount <= 0) return true
            return isOddRowHighlighted(entryCount - 1)
        }
    }

    private val _uiState = MutableStateFlow(
        AppUiState(
            selectedTab = storage.getSelectedTab(),
            visitedUrls = storage.getVisitedUrls(),
            visitedSites = storage.getVisitedSites(),
            apiBaseUrl = storage.getApiBaseUrl().also { apiClient.updateBaseUrl(it) },
            themeMode = storage.getThemeMode()
        )
    )
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    private var activeLoadJob: Job? = null

    fun selectTab(tab: FeedTab) {
        val current = _uiState.value
        val wasSameSelection = current.selectedTab == tab && current.selectedSite == null
        storage.setSelectedTab(tab)
        _uiState.update {
            it.copy(
                selectedTab = tab,
                selectedSite = null,
                isLoadingMore = false,
                loadMoreError = false,
                scrollResetVersion = it.scrollResetVersion + 1
            )
        }
        if (wasSameSelection || current.selectedTab != tab || current.selectedSite != null) {
            triggerLoad(reset = true)
        }
    }

    fun selectSite(site: String) {
        val cleaned = site.trim()
        if (cleaned.isEmpty()) return
        markSiteVisited(cleaned)
        _uiState.update {
            it.copy(
                selectedSite = cleaned,
                isLoadingMore = false,
                loadMoreError = false,
                scrollResetVersion = it.scrollResetVersion + 1
            )
        }
        triggerLoad(reset = true)
    }

    /**
     * Closes the active site/domain filter and navigates back to the Hottest section (`/api/hot`).
     */
    fun clearSiteFilter() {
        val current = _uiState.value
        if (current.selectedSite == null && current.selectedTab == FeedTab.HOT) return
        storage.setSelectedTab(FeedTab.HOT)
        _uiState.update {
            it.copy(
                selectedTab = FeedTab.HOT,
                selectedSite = null,
                isLoadingMore = false,
                loadMoreError = false,
                scrollResetVersion = it.scrollResetVersion + 1
            )
        }
        triggerLoad(reset = true)
    }

    fun refresh() {
        triggerLoad(reset = true)
    }

    fun triggerLoad(reset: Boolean): Job {
        if (reset) {
            activeLoadJob?.cancel()
        }
        val job = scope.launch {
            loadFeed(reset = reset)
        }
        if (reset) {
            activeLoadJob = job
        }
        return job
    }

    suspend fun loadFeed(reset: Boolean) {
        val snapshot = _uiState.value
        if (reset) {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    isLoadingMore = false,
                    loadMoreError = false,
                    errorMessage = null
                )
            }
        } else {
            if (!snapshot.canLoadMore || snapshot.isLoading || snapshot.isLoadingMore) {
                return
            }
            _uiState.update {
                it.copy(
                    isLoadingMore = true,
                    loadMoreError = false
                )
            }
        }

        val targetPage = if (reset) 1 else snapshot.currentPage + 1
        val activeSite = _uiState.value.selectedSite
        val activeTab = _uiState.value.selectedTab

        try {
            val response = if (!activeSite.isNullOrBlank()) {
                apiClient.fetchSite(site = activeSite, page = targetPage)
            } else {
                apiClient.fetchFeed(tab = activeTab, page = targetPage)
            }

            val latest = _uiState.value
            if (latest.selectedTab != activeTab || latest.selectedSite != activeSite) {
                return
            }

            val mergedEntries = if (reset) {
                response.entries
            } else {
                val existingIds = latest.entries.mapTo(mutableSetOf()) { it.id }
                val newUnique = response.entries.filter { it.id !in existingIds }
                latest.entries + newUnique
            }

            val resolvedTotalPages = max(
                response.resolvedPages,
                if (response.entries.isEmpty()) 0 else response.page
            )

            _uiState.update {
                it.copy(
                    entries = mergedEntries,
                    currentPage = response.page,
                    totalPages = resolvedTotalPages,
                    isLoading = false,
                    isLoadingMore = false,
                    loadMoreError = false,
                    errorMessage = null
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val latest = _uiState.value
            if (latest.selectedTab != activeTab || latest.selectedSite != activeSite) {
                return
            }
            if (reset) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        errorMessage = e.message ?: "Unable to load news feed"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoadingMore = false,
                        loadMoreError = true
                    )
                }
            }
        }
    }

    suspend fun loadMoreIfNeeded(currentEntry: NewsEntry) {
        val current = _uiState.value
        if (!current.canLoadMore || current.isLoading || current.isLoadingMore) return
        val index = current.entries.indexOfFirst { it.id == currentEntry.id }
        if (index < 0) return
        val thresholdIndex = max(0, current.entries.size - 3)
        if (index >= thresholdIndex) {
            loadFeed(reset = false)
        }
    }

    fun markArticleVisited(url: String) {
        val cleaned = url.trim()
        if (cleaned.isEmpty()) return
        val updated = _uiState.value.visitedUrls + cleaned
        storage.setVisitedUrls(updated)
        _uiState.update { it.copy(visitedUrls = updated) }
    }

    fun isArticleVisited(url: String): Boolean =
        _uiState.value.visitedUrls.contains(url.trim())

    fun markSiteVisited(site: String) {
        val cleaned = site.trim()
        if (cleaned.isEmpty()) return
        val updated = _uiState.value.visitedSites + cleaned
        storage.setVisitedSites(updated)
        _uiState.update { it.copy(visitedSites = updated) }
    }

    fun isSiteVisited(site: String): Boolean =
        _uiState.value.visitedSites.contains(site.trim())

    fun recordArticleOpened(entry: NewsEntry) {
        markArticleVisited(entry.url)
        markSiteVisited(entry.siteLabel)
    }

    fun updateApiBaseUrl(newUrl: String) {
        val normalized = APIClient.normalizeBaseUrl(newUrl)
        apiClient.updateBaseUrl(normalized)
        storage.setApiBaseUrl(normalized)
        _uiState.update { it.copy(apiBaseUrl = normalized) }
        triggerLoad(reset = true)
    }

    fun updateThemeMode(mode: ThemeMode) {
        storage.setThemeMode(mode)
        _uiState.update { it.copy(themeMode = mode) }
    }

    fun handleDeepLinkPath(path: String?) {
        val cleaned = path?.trim()?.trim('/') ?: ""
        when {
            cleaned.isEmpty() || cleaned.equals("api/hot", ignoreCase = true) -> {
                selectTab(FeedTab.HOT)
            }
            cleaned.equals("cold", ignoreCase = true) || cleaned.equals("api/cold", ignoreCase = true) -> {
                selectTab(FeedTab.COLD)
            }
            cleaned.equals("new", ignoreCase = true) || cleaned.equals("api/new", ignoreCase = true) -> {
                selectTab(FeedTab.NEW)
            }
            cleaned.startsWith("api/", ignoreCase = true) -> {
                val site = cleaned.removePrefix("api/").trim('/')
                if (site.isNotBlank()) {
                    selectSite(site)
                }
            }
            else -> {
                val site = cleaned.substringBefore('/')
                if (site.isNotBlank()) {
                    selectSite(site)
                }
            }
        }
    }
}

class AppStateViewModel(application: Application) : AndroidViewModel(application) {
    private val storage = SessionStorage(application)
    val apiClient = APIClient(baseUrl = storage.getApiBaseUrl())
    val controller = NewsStateController(
        storage = storage,
        apiClient = apiClient,
        scope = viewModelScope
    )

    val uiState: StateFlow<AppUiState> = controller.uiState

    init {
        controller.triggerLoad(reset = true)
    }

    fun selectTab(tab: FeedTab) = controller.selectTab(tab)
    fun selectSite(site: String) = controller.selectSite(site)
    fun clearSiteFilter() = controller.clearSiteFilter()
    fun refresh() = controller.refresh()
    suspend fun loadFeed(reset: Boolean) = controller.loadFeed(reset)
    suspend fun loadMoreIfNeeded(currentEntry: NewsEntry) = controller.loadMoreIfNeeded(currentEntry)
    fun recordArticleOpened(entry: NewsEntry) = controller.recordArticleOpened(entry)
    fun updateApiBaseUrl(newUrl: String) = controller.updateApiBaseUrl(newUrl)
    fun updateThemeMode(mode: ThemeMode) = controller.updateThemeMode(mode)

    fun handleDeepLink(intent: Intent?) {
        val data = intent?.data ?: return
        controller.handleDeepLinkPath(data.path)
    }
}
