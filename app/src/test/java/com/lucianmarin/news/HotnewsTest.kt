package com.lucianmarin.news

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.AccessTimeFilled
import androidx.compose.material.icons.rounded.HighlightOff
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.StickyNote2
import com.lucianmarin.news.model.FeedTab
import com.lucianmarin.news.model.NewsEntry
import com.lucianmarin.news.model.NewsFormatter
import com.lucianmarin.news.model.PaginatedNewsResponse
import com.lucianmarin.news.network.APIClient
import com.lucianmarin.news.state.InMemoryNewsPreferencesStore
import com.lucianmarin.news.state.NewsStateController
import com.lucianmarin.news.ui.navigation.AboutNavIcon
import com.lucianmarin.news.ui.navigation.ActiveSiteNavIcon
import com.lucianmarin.news.ui.navigation.ActiveWebNavIcon
import com.lucianmarin.news.ui.navigation.CloseSiteNavIcon
import com.lucianmarin.news.ui.navigation.NAV_BAR_FADE_DURATION_MS
import com.lucianmarin.news.ui.navigation.ShareWebNavIcon
import com.lucianmarin.news.ui.navigation.activeIcon
import com.lucianmarin.news.ui.screens.AboutContent
import com.lucianmarin.news.ui.screens.isOddNewsRow
import com.lucianmarin.news.ui.screens.shouldShowBottomDivider
import com.lucianmarin.news.ui.screens.shouldShowTopDivider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HotnewsTest {

    @Test
    fun `bottom navigation sections map to Hottest, Coldest, and Newest endpoints and active icons`() {
        assertEquals(listOf(FeedTab.HOT, FeedTab.COLD, FeedTab.NEW), FeedTab.entries)
        assertEquals("Hottest", FeedTab.HOT.title)
        assertEquals("Coldest", FeedTab.COLD.title)
        assertEquals("Newest", FeedTab.NEW.title)
        assertEquals("/api/hot", FeedTab.HOT.apiPath)
        assertEquals("/api/cold", FeedTab.COLD.apiPath)
        assertEquals("/api/new", FeedTab.NEW.apiPath)
        assertEquals(Icons.Rounded.LocalFireDepartment, FeedTab.HOT.activeIcon)
        assertEquals(Icons.Rounded.AcUnit, FeedTab.COLD.activeIcon)
        assertEquals(Icons.Rounded.AccessTimeFilled, FeedTab.NEW.activeIcon)
        assertEquals(Icons.Rounded.StickyNote2, ActiveSiteNavIcon)
        assertEquals(Icons.Rounded.Info, AboutNavIcon)
        assertEquals(Icons.Rounded.Public, ActiveWebNavIcon)
        assertEquals(Icons.Rounded.Share, ShareWebNavIcon)
        assertEquals(Icons.Rounded.HighlightOff, CloseSiteNavIcon)
        assertTrue(NAV_BAR_FADE_DURATION_MS > 0)
    }

    @Test
    fun `AboutContent builds 5 sections matching news about and privacy templates`() {
        val sections = AboutContent.buildSections(listOf("wired", "9to5mac", "9to5google"))
        assertEquals(5, sections.size)
        assertEquals(
            listOf(
                "About Hotnews",
                "Tech stack and support",
                "Features and design constraints",
                "3 publications",
                "Contact and privacy"
            ),
            sections.map { it.title }
        )
        assertTrue(sections[0].plainTextParagraphs.first().startsWith("Welcome to a minimalist news aggregator"))
        assertEquals(6, sections[1].plainTextParagraphs.size)
        assertEquals(10, sections[2].plainTextParagraphs.size)
        assertEquals("9to5google, 9to5mac, wired.", sections[3].plainTextParagraphs.single())
        assertEquals(3, sections[4].plainTextParagraphs.size)
        assertTrue(sections[4].plainTextParagraphs[0].contains("privacy policy"))
        assertEquals("Gmail — marin.lucian", sections[4].plainTextParagraphs[1])
        assertEquals("Phone — +40 726 210 589", sections[4].plainTextParagraphs[2])
    }

    @Test
    fun `decodes PaginatedNewsResponse and computes degrees, ageLabel, and siteLabel`() {
        val rawJson = """
            {
                "page": 1,
                "pages": 4,
                "view": "hottest",
                "entries": [
                    {
                        "id": "d3b07384d113edec49eaa6238ad5ff00",
                        "url": "https://www.macrumors.com/2026/10/28/top-stories/",
                        "title": "Top Stories: Apple 'Pro and Tell' Debut, iPhone Duo Launch, and More",
                        "domain": "macrumors.com",
                        "site": "macrumors",
                        "pub": 1793200000.0,
                        "age": "35h",
                        "author": "Juli Clover",
                        "description": "Yet another busy week for Apple saw a teaser for even more product announcements.",
                        "summary": "Yet another busy week for Apple saw a teaser for even more product announcements.",
                        "score": 0.0792,
                        "degrees": 79
                    }
                ]
            }
        """.trimIndent()

        val decoded = APIClient.json.decodeFromString<PaginatedNewsResponse>(rawJson)
        assertEquals(1, decoded.page)
        assertEquals(4, decoded.pages)
        assertEquals("hottest", decoded.view)
        assertTrue(decoded.hasNextPage)
        assertEquals(1, decoded.entries.size)

        val entry = decoded.entries.first()
        assertEquals("macrumors", entry.siteLabel)
        assertEquals("macrumors.com", entry.domainLabel)
        assertEquals("35h ago", entry.ageLabel)
        assertEquals(79, entry.resolvedDegrees)
        assertEquals("79°", entry.degreesLabel)
    }

    @Test
    fun `odd news rows are highlighted and even news rows are not`() {
        assertTrue(NewsStateController.isOddRowHighlighted(0))
        assertFalse(NewsStateController.isOddRowHighlighted(1))
        assertTrue(NewsStateController.isOddRowHighlighted(2))
        assertFalse(NewsStateController.isOddRowHighlighted(3))

        assertTrue(isOddNewsRow(0))
        assertFalse(isOddNewsRow(1))

        assertTrue(NewsStateController.isLastRowOddHighlighted(0))
        assertTrue(NewsStateController.isLastRowOddHighlighted(1))
        assertFalse(NewsStateController.isLastRowOddHighlighted(2))
        assertTrue(NewsStateController.isLastRowOddHighlighted(3))
        assertFalse(NewsStateController.isLastRowOddHighlighted(10))
    }

    @Test
    fun `first news story has no top divider and last news story has no bottom divider`() {
        assertFalse(shouldShowTopDivider(0))
        assertFalse(shouldShowTopDivider(1))

        assertTrue(shouldShowBottomDivider(zeroBasedIndex = 0, totalCount = 3))
        assertTrue(shouldShowBottomDivider(zeroBasedIndex = 1, totalCount = 3))
        assertFalse(shouldShowBottomDivider(zeroBasedIndex = 2, totalCount = 3))
        assertFalse(shouldShowBottomDivider(zeroBasedIndex = 0, totalCount = 1))
    }

    @Test
    fun `NewsFormatter mirrors shortdate, sitename, 480-char summary truncation, and site multiply label`() {
        val now = 1_700_000_000.0
        assertEquals("15m", NewsFormatter.shortDate(timestamp = now - 15 * 60, now = now))
        assertEquals("35h", NewsFormatter.shortDate(timestamp = now - 35 * 3600, now = now))

        assertEquals("macrumors", NewsFormatter.siteName("https://www.macrumors.com/story"))
        assertEquals("theregister", NewsFormatter.siteName("theregister.co.uk"))
        assertEquals("bair.berkeley", NewsFormatter.siteName("bair.berkeley.edu"))
        assertEquals("macrumors.com", NewsFormatter.domainName("https://www.macrumors.com/2026/10/28/top-stories/"))
        assertEquals("theregister.co.uk", NewsFormatter.domainName("https://www.theregister.co.uk/article"))

        val longText = "word ".repeat(120)
        val truncated = NewsFormatter.truncateSummary(longText, length = 480)
        assertTrue(truncated.length <= 480)
        assertTrue(truncated.endsWith("..."))

        assertEquals("macrumors", NewsFormatter.siteNavigationLabel("macrumors"))
        assertEquals(listOf("macrumors", "×"), NewsFormatter.siteNavigationOptions("macrumors"))
    }

    @Test
    fun `infinite scroll, pull-to-refresh, tab switching, and site filter close back to Hottest`() = runTest {
        val storage = InMemoryNewsPreferencesStore()
        val client = APIClient(
            baseUrl = "https://news.lucianmarin.com",
            transport = { url ->
                val page = url.query
                    ?.split("&")
                    ?.firstOrNull { it.startsWith("p=") }
                    ?.substringAfter("p=")
                    ?.toIntOrNull()
                    ?: 1
                val path = url.path
                val viewName = when (path) {
                    "/api/hot" -> "hottest"
                    "/api/cold" -> "coldest"
                    "/api/new" -> "newest"
                    else -> "site"
                }
                val siteName = if (viewName == "site") path.removePrefix("/api/") else null

                val entriesJson = (1..10).joinToString(",") { idx ->
                    val globalIndex = (page - 1) * 10 + idx
                    val site = siteName ?: "site$globalIndex"
                    """
                    {
                        "id": "$viewName-${siteName ?: "all"}-$globalIndex",
                        "url": "https://$site.com/article-$globalIndex",
                        "title": "$viewName Article $globalIndex",
                        "domain": "$site.com",
                        "site": "$site",
                        "pub": 1793200000.0,
                        "age": "${globalIndex}h",
                        "description": "Summary for article $globalIndex",
                        "score": 0.05
                    }
                    """.trimIndent()
                }

                val siteField = if (siteName != null) """"site": "$siteName",""" else ""
                val body = """
                    {
                        "page": $page,
                        "pages": 2,
                        "view": "$viewName",
                        $siteField
                        "entries": [$entriesJson]
                    }
                """.trimIndent()
                200 to body
            }
        )

        val controller = NewsStateController(
            storage = storage,
            apiClient = client,
            scope = this
        )

        // 1. Initial load of Hottest (page 1)
        controller.loadFeed(reset = true)
        assertEquals(FeedTab.HOT, controller.uiState.value.selectedTab)
        assertNull(controller.uiState.value.selectedSite)
        assertEquals(1, controller.uiState.value.currentPage)
        assertEquals(2, controller.uiState.value.totalPages)
        assertTrue(controller.uiState.value.canLoadMore)
        assertEquals(10, controller.uiState.value.entries.size)
        assertEquals("hottest-all-1", controller.uiState.value.entries.first().id)

        // 2. Infinite scroll near end of page 1 loads page 2
        val ninthEntry = controller.uiState.value.entries[8]
        controller.loadMoreIfNeeded(ninthEntry)
        assertEquals(2, controller.uiState.value.currentPage)
        assertFalse(controller.uiState.value.canLoadMore)
        assertEquals(20, controller.uiState.value.entries.size)
        assertEquals("hottest-all-20", controller.uiState.value.entries.last().id)

        // 3. Pull-to-refresh resets to page 1
        controller.loadFeed(reset = true)
        assertEquals(1, controller.uiState.value.currentPage)
        assertEquals(10, controller.uiState.value.entries.size)

        // 4. Switch bottom navigation to Coldest
        controller.selectTab(FeedTab.COLD)
        controller.loadFeed(reset = true)
        assertEquals(FeedTab.COLD, controller.uiState.value.selectedTab)
        assertEquals("coldest-all-1", controller.uiState.value.entries.first().id)

        // 5. Switch bottom navigation to Newest
        controller.selectTab(FeedTab.NEW)
        controller.loadFeed(reset = true)
        assertEquals(FeedTab.NEW, controller.uiState.value.selectedTab)
        assertEquals("newest-all-1", controller.uiState.value.entries.first().id)

        // 6. Select a site/domain while in Newest -> shows site feed
        controller.selectSite("macrumors")
        controller.loadFeed(reset = true)
        assertEquals("macrumors", controller.uiState.value.selectedSite)
        assertTrue(controller.uiState.value.isSiteSelected)
        assertFalse(controller.uiState.value.isAboutOpen)
        assertEquals("/api/macrumors", controller.uiState.value.activeFeedPath)
        assertEquals("site-macrumors-1", controller.uiState.value.entries.first().id)

        // 6b. Open About page while site filter feed is open -> fetches /api/new across all pages
        controller.openAboutPage()
        controller.loadPublications()
        assertTrue(controller.uiState.value.isAboutOpen)
        assertEquals("macrumors", controller.uiState.value.selectedSite)
        assertTrue(controller.uiState.value.canNavigateBack)
        assertEquals(20, controller.uiState.value.publications.size)
        assertEquals(
            (1..20).map { "site$it" }.sorted(),
            controller.uiState.value.publications
        )

        // Pressing OS back (or tapping StickyNote2 icon) from About returns to the site filter feed
        controller.navigateBack()
        assertFalse(controller.uiState.value.isAboutOpen)
        assertEquals("macrumors", controller.uiState.value.selectedSite)

        // 7. Closing the site/domain filter goes back to Hottest section
        controller.clearSiteFilter()
        controller.loadFeed(reset = true)
        assertNull(controller.uiState.value.selectedSite)
        assertFalse(controller.uiState.value.isSiteSelected)
        assertEquals(FeedTab.HOT, controller.uiState.value.selectedTab)
        assertEquals("/api/hot", controller.uiState.value.activeFeedPath)
        assertEquals("hottest-all-1", controller.uiState.value.entries.first().id)

        // 8. Opening an article opens in-app web page with site domain and URL for sharing
        val firstEntry = controller.uiState.value.entries.first()
        controller.openArticle(firstEntry)
        assertTrue(controller.uiState.value.isWebPageOpen)
        assertEquals("https://site1.com/article-1", controller.uiState.value.activeWebUrl)
        assertEquals("site1.com", controller.uiState.value.resolvedWebDomain)
        assertEquals("site1", controller.uiState.value.resolvedWebSite)
        assertTrue(controller.isArticleVisited(firstEntry.url))

        // Navigating within WebView updates active URL and domain
        controller.updateActiveWebUrl("https://www.macrumors.com/2026/11/14/new-post/")
        assertEquals("https://www.macrumors.com/2026/11/14/new-post/", controller.uiState.value.activeWebUrl)
        assertEquals("macrumors.com", controller.uiState.value.resolvedWebDomain)
        assertEquals("macrumors", controller.uiState.value.resolvedWebSite)

        // Tapping domain in web page nav bar closes web page, saves it in history, and filters feed by that site
        controller.selectSiteFromWebPage()
        controller.loadFeed(reset = true)
        assertFalse(controller.uiState.value.isWebPageOpen)
        assertNull(controller.uiState.value.activeWebUrl)
        assertEquals("https://www.macrumors.com/2026/11/14/new-post/", controller.uiState.value.retainedWebUrl)
        assertEquals("macrumors", controller.uiState.value.selectedSite)
        assertEquals("site-macrumors-1", controller.uiState.value.entries.first().id)

        // Pressing OS back from the site feed restores the web page from history instead of going to front page
        controller.navigateBack()
        assertTrue(controller.uiState.value.isWebPageOpen)
        assertEquals("https://www.macrumors.com/2026/11/14/new-post/", controller.uiState.value.activeWebUrl)
        assertEquals("macrumors.com", controller.uiState.value.resolvedWebDomain)
        assertNull(controller.uiState.value.selectedSite)
        assertEquals(FeedTab.HOT, controller.uiState.value.selectedTab)
        assertEquals("hottest-all-1", controller.uiState.value.entries.first().id)

        // Pressing OS back again from the web page closes it and returns to the front page
        controller.navigateBack()
        assertFalse(controller.uiState.value.isWebPageOpen)
        assertNull(controller.uiState.value.retainedWebUrl)
        assertNull(controller.uiState.value.selectedSite)
        assertEquals(FeedTab.HOT, controller.uiState.value.selectedTab)
        assertEquals("hottest-all-1", controller.uiState.value.entries.first().id)
    }

    @Test
    fun `endpoint caching stores only first page per endpoint and treats p=1 equal to plain endpoint`() = runTest {
        val storage = InMemoryNewsPreferencesStore()
        var version = 1
        val client = APIClient(
            baseUrl = "https://news.lucianmarin.com",
            cacheStore = storage,
            transport = { url ->
                val page = url.query
                    ?.split("&")
                    ?.firstOrNull { it.startsWith("p=") }
                    ?.substringAfter("p=")
                    ?.toIntOrNull()
                    ?: 1
                val path = url.path
                val body = """
                    {
                        "page": $page,
                        "pages": 3,
                        "view": "hottest",
                        "entries": [
                            {
                                "id": "$path-p$page-v$version",
                                "url": "https://example.com$path/$page/$version",
                                "title": "Entry $path p$page v$version",
                                "domain": "example.com",
                                "site": "example",
                                "pub": 1793200000.0,
                                "age": "1h",
                                "description": "Summary",
                                "score": 0.1
                            }
                        ]
                    }
                """.trimIndent()
                200 to body
            }
        )

        // ?p=1 URL is normalized to plain endpoint URL without p parameters
        assertEquals(
            "https://news.lucianmarin.com/api/hot",
            client.buildUrl("/api/hot").toString()
        )
        assertEquals(
            "https://news.lucianmarin.com/api/hot",
            client.buildUrl("/api/hot?p=1").toString()
        )
        assertEquals(
            "https://news.lucianmarin.com/api/hot?p=2",
            client.buildUrl("/api/hot", page = 2).toString()
        )

        // Cache key normalization: ?p=1 equals plain endpoint; page > 1 returns null
        assertEquals("/api/hot", APIClient.cacheKeyForEndpoint("/api/hot"))
        assertEquals("/api/hot", APIClient.cacheKeyForEndpoint("/api/hot?p=1"))
        assertEquals("/api/hot", APIClient.cacheKeyForEndpoint("https://news.lucianmarin.com/api/hot?p=1"))
        assertNull(APIClient.cacheKeyForEndpoint("/api/hot?p=2"))
        assertNull(APIClient.cacheKeyForEndpoint("/api/hot", page = 2))

        // Fetch /api/hot?p=1 -> caches under /api/hot
        client.fetchEntries("/api/hot?p=1")
        val hotCache = storage.getEndpointCache("/api/hot")
        val hotCacheViaP1 = storage.getEndpointCache("/api/hot?p=1")
        assertEquals(hotCache, hotCacheViaP1)
        assertEquals("/api/hot-p1-v1", hotCache?.entries?.first()?.id)

        // Fetch page 2 -> must NOT overwrite page 1 cache or create a page 2 cache entry
        version = 2
        client.fetchFeed(FeedTab.HOT, page = 2)
        assertNull(storage.getEndpointCache("/api/hot?p=2"))
        assertNull(client.getCachedResponse("/api/hot", page = 2))
        assertEquals("/api/hot-p1-v1", storage.getEndpointCache("/api/hot")?.entries?.first()?.id)

        // Fetch other endpoints -> stored separately per API endpoint
        client.fetchFeed(FeedTab.COLD, page = 1)
        client.fetchSite("macrumors", page = 1)
        assertEquals(
            setOf("/api/hot", "/api/cold", "/api/macrumors"),
            storage.getAllEndpointCaches().keys
        )
        assertEquals("/api/cold-p1-v2", storage.getEndpointCache("/api/cold")?.entries?.first()?.id)
        assertEquals("/api/macrumors-p1-v2", storage.getEndpointCache("/api/macrumors")?.entries?.first()?.id)
    }

    @Test
    fun `draws cached content on first paint and replaces in place when endpoint content is ready`() = runTest {
        val cachedHotResponse = PaginatedNewsResponse(
            page = 1,
            pages = 2,
            view = "hottest",
            entries = listOf(
                NewsEntry(
                    id = "cached-hot-1",
                    url = "https://example.com/cached-hot-1",
                    title = "Cached Hot Story",
                    domain = "example.com",
                    site = "example",
                    age = "2h",
                    description = "Cached summary",
                    score = 0.08
                )
            )
        )
        val cachedColdResponse = PaginatedNewsResponse(
            page = 1,
            pages = 2,
            view = "coldest",
            entries = listOf(
                NewsEntry(
                    id = "cached-cold-1",
                    url = "https://example.com/cached-cold-1",
                    title = "Cached Cold Story",
                    domain = "example.com",
                    site = "example",
                    age = "4h",
                    description = "Cached cold summary",
                    score = 0.01
                )
            )
        )

        val storage = InMemoryNewsPreferencesStore(
            initialEndpointCaches = mapOf(
                "/api/hot" to cachedHotResponse,
                "/api/cold?p=1" to cachedColdResponse
            )
        )

        val client = APIClient(
            baseUrl = "https://news.lucianmarin.com",
            cacheStore = storage,
            transport = { url ->
                val path = url.path
                val freshId = when (path) {
                    "/api/hot" -> "fresh-hot-1"
                    "/api/cold" -> "fresh-cold-1"
                    else -> "fresh-other-1"
                }
                val body = """
                    {
                        "page": 1,
                        "pages": 3,
                        "view": "hottest",
                        "entries": [
                            {
                                "id": "$freshId",
                                "url": "https://example.com/$freshId",
                                "title": "Fresh Story $freshId",
                                "domain": "example.com",
                                "site": "example",
                                "pub": 1793200000.0,
                                "age": "1h",
                                "description": "Fresh summary",
                                "score": 0.09
                            }
                        ]
                    }
                """.trimIndent()
                200 to body
            }
        )

        val controller = NewsStateController(
            storage = storage,
            apiClient = client,
            scope = this
        )

        // First paint before network call runs: immediately has cached /api/hot entries
        assertEquals(listOf("cached-hot-1"), controller.uiState.value.entries.map { it.id })
        assertTrue(controller.uiState.value.isShowingCachedContent)

        // When /api/hot endpoint content is ready, it draws in place of cache
        controller.loadFeed(reset = true)
        assertEquals(listOf("fresh-hot-1"), controller.uiState.value.entries.map { it.id })
        assertFalse(controller.uiState.value.isShowingCachedContent)
        assertEquals("fresh-hot-1", storage.getEndpointCache("/api/hot")?.entries?.first()?.id)

        // Switching to Coldest immediately draws cached /api/cold on first paint before network completes
        controller.selectTab(FeedTab.COLD)
        assertEquals(listOf("cached-cold-1"), controller.uiState.value.entries.map { it.id })
        assertTrue(controller.uiState.value.isShowingCachedContent)

        // When /api/cold endpoint content is ready, it draws in place of cache
        controller.loadFeed(reset = true)
        assertEquals(listOf("fresh-cold-1"), controller.uiState.value.entries.map { it.id })
        assertFalse(controller.uiState.value.isShowingCachedContent)
    }
}
