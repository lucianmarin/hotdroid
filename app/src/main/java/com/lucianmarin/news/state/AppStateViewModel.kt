package com.lucianmarin.news.state

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lucianmarin.news.model.FeedTab
import com.lucianmarin.news.model.NewsEntry
import com.lucianmarin.news.model.NewsFormatter
import com.lucianmarin.news.model.PaginatedNewsResponse
import com.lucianmarin.news.network.APIClient
import com.lucianmarin.news.ui.screens.AboutContent
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

data class NavigationHistoryEntry(
    val selectedTab: FeedTab,
    val selectedSite: String?,
    val isAboutOpen: Boolean = false,
    val entries: List<NewsEntry>,
    val currentPage: Int,
    val totalPages: Int,
    val isShowingCachedContent: Boolean,
    val activeWebUrl: String?,
    val activeWebDomain: String?,
    val activeWebSite: String?
)

data class AppUiState(
    val selectedTab: FeedTab = FeedTab.HOT,
    val selectedSite: String? = null,
    val isAboutOpen: Boolean = false,
    val publications: List<String> = AboutContent.DEFAULT_PUBLICATIONS,
    val entries: List<NewsEntry> = emptyList(),
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val isShowingCachedContent: Boolean = false,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val loadMoreError: Boolean = false,
    val errorMessage: String? = null,
    val visitedUrls: Set<String> = emptySet(),
    val visitedSites: Set<String> = emptySet(),
    val apiBaseUrl: String = APIClient.DEFAULT_BASE_URL,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val scrollResetVersion: Int = 0,
    val activeWebUrl: String? = null,
    val activeWebDomain: String? = null,
    val activeWebSite: String? = null,
    val navigationHistory: List<NavigationHistoryEntry> = emptyList()
) {
    val canLoadMore: Boolean
        get() = currentPage < totalPages && entries.isNotEmpty()

    val isSiteSelected: Boolean
        get() = !selectedSite.isNullOrBlank()

    val isWebPageOpen: Boolean
        get() = !activeWebUrl.isNullOrBlank()

    val canNavigateBack: Boolean
        get() = isWebPageOpen || isAboutOpen || navigationHistory.isNotEmpty() || isSiteSelected

    val retainedWebUrl: String?
        get() = activeWebUrl?.takeIf { it.isNotBlank() }
            ?: navigationHistory.lastOrNull { !it.activeWebUrl.isNullOrBlank() }?.activeWebUrl

    val resolvedWebDomain: String?
        get() = activeWebDomain?.takeIf { it.isNotBlank() }
            ?: activeWebUrl?.let { NewsFormatter.domainName(it) }?.takeIf { it.isNotBlank() }

    val resolvedWebSite: String?
        get() = activeWebSite?.takeIf { it.isNotBlank() }
            ?: resolvedWebDomain?.let { NewsFormatter.siteName(it) }?.takeIf { it.isNotBlank() }

    val activeFeedPath: String
        get() = if (!selectedSite.isNullOrBlank()) {
            APIClient.siteEndpointPath(selectedSite)
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

        fun endpointPathForSelection(selectedTab: FeedTab, selectedSite: String?): String =
            if (!selectedSite.isNullOrBlank()) {
                APIClient.siteEndpointPath(selectedSite)
            } else {
                selectedTab.apiPath
            }

        fun resolveTotalPages(response: PaginatedNewsResponse): Int =
            max(
                response.resolvedPages,
                if (response.entries.isEmpty()) 0 else response.page
            )
    }

    private val _uiState: MutableStateFlow<AppUiState> = run {
        apiClient.attachCacheStore(storage)
        val initialTab = storage.getSelectedTab()
        val initialCache = getCachedResponseForSelection(selectedTab = initialTab, selectedSite = null)
        MutableStateFlow(
            AppUiState(
                selectedTab = initialTab,
                entries = initialCache?.entries ?: emptyList(),
                currentPage = initialCache?.page ?: 1,
                totalPages = initialCache?.let { resolveTotalPages(it) } ?: 1,
                isShowingCachedContent = initialCache != null,
                visitedUrls = storage.getVisitedUrls(),
                visitedSites = storage.getVisitedSites(),
                apiBaseUrl = storage.getApiBaseUrl().also { apiClient.updateBaseUrl(it) },
                themeMode = storage.getThemeMode()
            )
        )
    }
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    private var activeLoadJob: Job? = null
    private var publicationsLoadJob: Job? = null
    private var hasLoadedPublicationsFromApi: Boolean = false

    fun getCachedResponseForEndpoint(endpoint: String, page: Int = 1): PaginatedNewsResponse? {
        val key = APIClient.cacheKeyForEndpoint(endpoint, page) ?: return null
        return apiClient.getCachedResponse(key, page = 1) ?: storage.getEndpointCache(key)
    }

    private fun getCachedResponseForSelection(
        selectedTab: FeedTab,
        selectedSite: String?
    ): PaginatedNewsResponse? {
        val endpoint = try {
            endpointPathForSelection(selectedTab, selectedSite)
        } catch (_: Exception) {
            return null
        }
        return getCachedResponseForEndpoint(endpoint, page = 1)
    }

    private fun cacheFirstPageResponse(
        selectedTab: FeedTab,
        selectedSite: String?,
        response: PaginatedNewsResponse
    ) {
        if (response.page != 1) return
        val endpoint = try {
            endpointPathForSelection(selectedTab, selectedSite)
        } catch (_: Exception) {
            return
        }
        apiClient.cacheResponse(path = endpoint, response = response, page = 1)
        storage.setEndpointCache(endpoint = endpoint, response = response)
    }

    fun selectTab(tab: FeedTab) {
        val current = _uiState.value
        val wasSameSelection = current.selectedTab == tab && current.selectedSite == null
        val cached = getCachedResponseForSelection(selectedTab = tab, selectedSite = null)
        storage.setSelectedTab(tab)
        _uiState.update { state ->
            val nextEntries = when {
                cached != null -> cached.entries
                !wasSameSelection -> emptyList()
                else -> state.entries
            }
            val nextCurrentPage = when {
                cached != null -> cached.page
                !wasSameSelection -> 1
                else -> state.currentPage
            }
            val nextTotalPages = when {
                cached != null -> resolveTotalPages(cached)
                !wasSameSelection -> 1
                else -> state.totalPages
            }
            state.copy(
                selectedTab = tab,
                selectedSite = null,
                isAboutOpen = false,
                activeWebUrl = null,
                activeWebDomain = null,
                activeWebSite = null,
                navigationHistory = emptyList(),
                entries = nextEntries,
                currentPage = nextCurrentPage,
                totalPages = nextTotalPages,
                isShowingCachedContent = cached != null,
                isLoadingMore = false,
                loadMoreError = false,
                errorMessage = null,
                scrollResetVersion = state.scrollResetVersion + 1
            )
        }
        if (wasSameSelection || current.selectedTab != tab || current.selectedSite != null || current.isAboutOpen) {
            triggerLoad(reset = true)
        }
    }

    fun selectSite(site: String) {
        val cleaned = site.trim()
        if (cleaned.isEmpty()) return
        val current = _uiState.value
        val wasSameSite = current.selectedSite == cleaned
        val cached = getCachedResponseForSelection(selectedTab = current.selectedTab, selectedSite = cleaned)
        markSiteVisited(cleaned)
        _uiState.update { state ->
            val nextEntries = when {
                cached != null -> cached.entries
                !wasSameSite -> emptyList()
                else -> state.entries
            }
            val nextCurrentPage = when {
                cached != null -> cached.page
                !wasSameSite -> 1
                else -> state.currentPage
            }
            val nextTotalPages = when {
                cached != null -> resolveTotalPages(cached)
                !wasSameSite -> 1
                else -> state.totalPages
            }
            state.copy(
                selectedSite = cleaned,
                isAboutOpen = false,
                activeWebUrl = null,
                activeWebDomain = null,
                activeWebSite = null,
                entries = nextEntries,
                currentPage = nextCurrentPage,
                totalPages = nextTotalPages,
                isShowingCachedContent = cached != null,
                isLoadingMore = false,
                loadMoreError = false,
                errorMessage = null,
                scrollResetVersion = state.scrollResetVersion + 1
            )
        }
        triggerLoad(reset = true)
    }

    /**
     * Closes the active site/domain filter and navigates back to the Hottest section (`/api/hot`).
     */
    fun clearSiteFilter() {
        val current = _uiState.value
        if (current.selectedSite == null && current.selectedTab == FeedTab.HOT && !current.isWebPageOpen && !current.isAboutOpen) return
        val cached = getCachedResponseForSelection(selectedTab = FeedTab.HOT, selectedSite = null)
        storage.setSelectedTab(FeedTab.HOT)
        _uiState.update { state ->
            state.copy(
                selectedTab = FeedTab.HOT,
                selectedSite = null,
                isAboutOpen = false,
                activeWebUrl = null,
                activeWebDomain = null,
                activeWebSite = null,
                navigationHistory = emptyList(),
                entries = cached?.entries ?: emptyList(),
                currentPage = cached?.page ?: 1,
                totalPages = cached?.let { resolveTotalPages(it) } ?: 1,
                isShowingCachedContent = cached != null,
                isLoadingMore = false,
                loadMoreError = false,
                errorMessage = null,
                scrollResetVersion = state.scrollResetVersion + 1
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
        val activeSite = snapshot.selectedSite
        val activeTab = snapshot.selectedTab

        if (reset) {
            val cached = getCachedResponseForSelection(selectedTab = activeTab, selectedSite = activeSite)
            _uiState.update { state ->
                if (state.entries.isEmpty() && cached != null) {
                    state.copy(
                        entries = cached.entries,
                        currentPage = cached.page,
                        totalPages = resolveTotalPages(cached),
                        isShowingCachedContent = true,
                        isLoading = true,
                        isLoadingMore = false,
                        loadMoreError = false,
                        errorMessage = null
                    )
                } else {
                    state.copy(
                        isLoading = true,
                        isLoadingMore = false,
                        loadMoreError = false,
                        errorMessage = null
                    )
                }
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

        try {
            val response = if (!activeSite.isNullOrBlank()) {
                apiClient.fetchSite(site = activeSite, page = targetPage)
            } else {
                apiClient.fetchFeed(tab = activeTab, page = targetPage)
            }

            if (targetPage == 1 && response.page == 1) {
                cacheFirstPageResponse(
                    selectedTab = activeTab,
                    selectedSite = activeSite,
                    response = response
                )
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

            val resolvedTotalPages = resolveTotalPages(response)

            _uiState.update {
                it.copy(
                    entries = mergedEntries,
                    currentPage = response.page,
                    totalPages = resolvedTotalPages,
                    isShowingCachedContent = false,
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

    fun openAboutPage() {
        _uiState.update {
            it.copy(
                isAboutOpen = true,
                activeWebUrl = null,
                activeWebDomain = null,
                activeWebSite = null
            )
        }
        if (!hasLoadedPublicationsFromApi && publicationsLoadJob?.isActive != true) {
            publicationsLoadJob = scope.launch {
                loadPublications()
            }
        }
    }

    suspend fun loadPublications(force: Boolean = false): List<String> {
        if (hasLoadedPublicationsFromApi && !force) {
            return _uiState.value.publications
        }
        return try {
            val fetched = apiClient.fetchPublications { partialSites ->
                if (partialSites.isNotEmpty()) {
                    _uiState.update { it.copy(publications = partialSites) }
                }
            }
            if (fetched.isNotEmpty()) {
                hasLoadedPublicationsFromApi = true
                _uiState.update { it.copy(publications = fetched) }
                fetched
            } else {
                _uiState.value.publications
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            _uiState.value.publications
        }
    }

    fun closeAboutPage() {
        _uiState.update {
            it.copy(
                isAboutOpen = false
            )
        }
    }

    fun openWebUrl(url: String) {
        val cleanUrl = url.trim()
        if (cleanUrl.isEmpty()) return
        if (cleanUrl == "/") {
            clearSiteFilter()
            return
        }
        if (cleanUrl.startsWith("/")) {
            val site = cleanUrl.removePrefix("/").trim()
            if (site.isNotEmpty()) {
                selectSite(site)
            }
            return
        }
        markArticleVisited(cleanUrl)
        val domain = NewsFormatter.domainName(cleanUrl)
        val site = NewsFormatter.siteName(domain.ifBlank { cleanUrl })
        _uiState.update {
            it.copy(
                activeWebUrl = cleanUrl,
                activeWebDomain = domain,
                activeWebSite = site
            )
        }
    }

    fun openArticle(entry: NewsEntry) {
        val cleanUrl = entry.url.trim()
        if (cleanUrl.isEmpty()) return
        recordArticleOpened(entry)
        val domain = entry.domainLabel.ifBlank { NewsFormatter.domainName(cleanUrl) }
        val site = entry.siteLabel.ifBlank { NewsFormatter.siteName(domain.ifBlank { cleanUrl }) }
        _uiState.update {
            it.copy(
                activeWebUrl = cleanUrl,
                activeWebDomain = domain,
                activeWebSite = site
            )
        }
    }

    fun updateActiveWebUrl(url: String) {
        val cleanUrl = url.trim()
        if (cleanUrl.isEmpty()) return
        val domain = NewsFormatter.domainName(cleanUrl)
        val site = NewsFormatter.siteName(domain.ifBlank { cleanUrl })
        _uiState.update { state ->
            if (!state.isWebPageOpen) {
                state
            } else {
                state.copy(
                    activeWebUrl = cleanUrl,
                    activeWebDomain = domain.ifBlank { state.activeWebDomain },
                    activeWebSite = site.ifBlank { state.activeWebSite }
                )
            }
        }
    }

    fun closeWebPage() {
        _uiState.update {
            it.copy(
                activeWebUrl = null,
                activeWebDomain = null,
                activeWebSite = null
            )
        }
    }

    fun selectSiteFromWebPage() {
        val current = _uiState.value
        val targetSite = current.resolvedWebSite ?: return
        val cleaned = targetSite.trim()
        if (cleaned.isEmpty()) return

        val historySnapshot = NavigationHistoryEntry(
            selectedTab = current.selectedTab,
            selectedSite = current.selectedSite,
            isAboutOpen = current.isAboutOpen,
            entries = current.entries,
            currentPage = current.currentPage,
            totalPages = current.totalPages,
            isShowingCachedContent = current.isShowingCachedContent,
            activeWebUrl = current.activeWebUrl,
            activeWebDomain = current.activeWebDomain,
            activeWebSite = current.activeWebSite
        )

        val wasSameSite = current.selectedSite == cleaned
        val cached = getCachedResponseForSelection(selectedTab = current.selectedTab, selectedSite = cleaned)
        markSiteVisited(cleaned)
        _uiState.update { state ->
            val nextEntries = when {
                cached != null -> cached.entries
                !wasSameSite -> emptyList()
                else -> state.entries
            }
            val nextCurrentPage = when {
                cached != null -> cached.page
                !wasSameSite -> 1
                else -> state.currentPage
            }
            val nextTotalPages = when {
                cached != null -> resolveTotalPages(cached)
                !wasSameSite -> 1
                else -> state.totalPages
            }
            state.copy(
                selectedSite = cleaned,
                isAboutOpen = false,
                activeWebUrl = null,
                activeWebDomain = null,
                activeWebSite = null,
                navigationHistory = state.navigationHistory + historySnapshot,
                entries = nextEntries,
                currentPage = nextCurrentPage,
                totalPages = nextTotalPages,
                isShowingCachedContent = cached != null,
                isLoadingMore = false,
                loadMoreError = false,
                errorMessage = null,
                scrollResetVersion = state.scrollResetVersion + 1
            )
        }
        triggerLoad(reset = true)
    }

    /**
     * Handles OS Back navigation:
     * - If a web page is open, closes the web page and returns to the feed or About page behind it.
     * - If the About page is open, closes the About page and returns to the site filter feed.
     * - If navigation history has a saved entry (e.g. a web page from which the domain pill was clicked),
     *   restores that web page and its underlying feed state.
     * - Otherwise, if a site filter is active, clears the site filter back to Hottest.
     */
    fun navigateBack() {
        val current = _uiState.value
        if (current.isWebPageOpen) {
            closeWebPage()
            return
        }
        if (current.isAboutOpen) {
            closeAboutPage()
            return
        }
        val previous = current.navigationHistory.lastOrNull()
        if (previous != null) {
            activeLoadJob?.cancel()
            storage.setSelectedTab(previous.selectedTab)
            _uiState.update { state ->
                state.copy(
                    selectedTab = previous.selectedTab,
                    selectedSite = previous.selectedSite,
                    isAboutOpen = previous.isAboutOpen,
                    entries = previous.entries,
                    currentPage = previous.currentPage,
                    totalPages = previous.totalPages,
                    isShowingCachedContent = previous.isShowingCachedContent,
                    isLoading = false,
                    isLoadingMore = false,
                    loadMoreError = false,
                    errorMessage = null,
                    activeWebUrl = previous.activeWebUrl,
                    activeWebDomain = previous.activeWebDomain,
                    activeWebSite = previous.activeWebSite,
                    navigationHistory = state.navigationHistory.dropLast(1)
                )
            }
            if (previous.entries.isEmpty() && !previous.isAboutOpen) {
                triggerLoad(reset = true)
            }
            return
        }
        if (current.isSiteSelected) {
            clearSiteFilter()
        }
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
    val apiClient = APIClient(
        baseUrl = storage.getApiBaseUrl(),
        cacheStore = storage
    )
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
    fun openAboutPage() = controller.openAboutPage()

    fun closeAboutPage() = controller.closeAboutPage()

    fun openWebUrl(url: String) = controller.openWebUrl(url)

    fun openArticle(entry: NewsEntry) = controller.openArticle(entry)
    fun updateActiveWebUrl(url: String) = controller.updateActiveWebUrl(url)
    fun closeWebPage() = controller.closeWebPage()
    fun selectSiteFromWebPage() = controller.selectSiteFromWebPage()
    fun navigateBack() = controller.navigateBack()
    fun updateApiBaseUrl(newUrl: String) = controller.updateApiBaseUrl(newUrl)
    fun updateThemeMode(mode: ThemeMode) = controller.updateThemeMode(mode)

    fun handleDeepLink(intent: Intent?) {
        val data = intent?.data ?: return
        controller.handleDeepLinkPath(data.path)
    }
}
