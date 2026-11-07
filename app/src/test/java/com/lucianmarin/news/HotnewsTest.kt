package com.lucianmarin.news

import com.lucianmarin.news.model.FeedTab
import com.lucianmarin.news.model.NewsEntry
import com.lucianmarin.news.model.NewsFormatter
import com.lucianmarin.news.model.PaginatedNewsResponse
import com.lucianmarin.news.network.APIClient
import com.lucianmarin.news.state.InMemoryNewsPreferencesStore
import com.lucianmarin.news.state.NewsStateController
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
    fun `bottom navigation sections map to Hottest, Coldest, and Newest endpoints`() {
        assertEquals(listOf(FeedTab.HOT, FeedTab.COLD, FeedTab.NEW), FeedTab.entries)
        assertEquals("Hottest", FeedTab.HOT.title)
        assertEquals("Coldest", FeedTab.COLD.title)
        assertEquals("Newest", FeedTab.NEW.title)
        assertEquals("/api/hot", FeedTab.HOT.apiPath)
        assertEquals("/api/cold", FeedTab.COLD.apiPath)
        assertEquals("/api/new", FeedTab.NEW.apiPath)
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
        assertEquals("/api/macrumors", controller.uiState.value.activeFeedPath)
        assertEquals("site-macrumors-1", controller.uiState.value.entries.first().id)

        // 7. Closing the site/domain filter goes back to Hottest section
        controller.clearSiteFilter()
        controller.loadFeed(reset = true)
        assertNull(controller.uiState.value.selectedSite)
        assertFalse(controller.uiState.value.isSiteSelected)
        assertEquals(FeedTab.HOT, controller.uiState.value.selectedTab)
        assertEquals("/api/hot", controller.uiState.value.activeFeedPath)
        assertEquals("hottest-all-1", controller.uiState.value.entries.first().id)
    }
}
