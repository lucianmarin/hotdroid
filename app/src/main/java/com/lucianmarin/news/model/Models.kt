package com.lucianmarin.news.model

import kotlinx.serialization.Serializable
import java.net.URI
import kotlin.math.max
import kotlin.math.roundToInt

enum class FeedTab(
    val id: String,
    val title: String,
    val apiPath: String,
    val viewName: String
) {
    HOT(
        id = "hot",
        title = "Hottest",
        apiPath = "/api/hot",
        viewName = "hottest"
    ),
    COLD(
        id = "cold",
        title = "Coldest",
        apiPath = "/api/cold",
        viewName = "coldest"
    ),
    NEW(
        id = "new",
        title = "Newest",
        apiPath = "/api/new",
        viewName = "newest"
    );

    companion object {
        fun fromId(id: String?): FeedTab =
            entries.find { it.id.equals(id, ignoreCase = true) || it.viewName.equals(id, ignoreCase = true) }
                ?: HOT
    }
}

object NewsFormatter {
    const val MULTIPLY_SIGN: String = "\u00D7"

    private val TWO_PART_TLDS = setOf(
        "co.uk", "org.uk", "ac.uk",
        "co.jp", "ne.jp", "or.jp",
        "com.au", "com.br"
    )

    /**
     * Mirrors `shortdate(timestamp)` in `../news/app/filters.py` and `NewsFormatter.shortDate` in `../macnews`.
     */
    fun shortDate(
        timestamp: Double,
        now: Double = System.currentTimeMillis() / 1000.0
    ): String {
        val totalSeconds = now - timestamp
        val minutes = (totalSeconds / 60.0).roundToInt()
        val hours = (totalSeconds / 3600.0).roundToInt()
        return if (minutes < 60) {
            "${max(0, minutes)}m"
        } else {
            "${max(1, hours)}h"
        }
    }

    /**
     * Mirrors Jinja2 `truncate(480, False, '...')` in `../news/templates/entry.html` and `../macnews`.
     */
    fun truncateSummary(
        text: String,
        length: Int = 480,
        leeway: Int = 5,
        ellipsis: String = "..."
    ): String {
        val cleaned = text.trim()
        if (cleaned.length <= length + leeway) {
            return cleaned
        }
        val targetLength = max(0, length - ellipsis.length)
        val prefix = cleaned.take(targetLength)
        val lastWhitespaceIndex = prefix.indexOfLast { it.isWhitespace() }
        if (lastWhitespaceIndex > 0) {
            val wordAligned = prefix.substring(0, lastWhitespaceIndex).trim()
            if (wordAligned.isNotEmpty()) {
                return wordAligned + ellipsis
            }
        }
        return prefix + ellipsis
    }

    /**
     * Extracts the clean domain name (e.g. `macrumors.com`, `theregister.co.uk`) from a domain or URL.
     */
    fun domainName(fromDomainOrUrl: String): String {
        var host = fromDomainOrUrl.trim()
        if (host.contains("://")) {
            try {
                val parsedHost = URI(host).host
                if (!parsedHost.isNullOrBlank()) {
                    host = parsedHost
                } else {
                    host = host.substringAfter("://").substringBefore("/")
                }
            } catch (_: Exception) {
                host = host.substringAfter("://").substringBefore("/")
            }
        } else if (host.contains("/")) {
            host = host.substringBefore("/")
        }
        host = host.substringBefore("?").substringBefore("#").substringBefore(":")
        if (host.lowercase().startsWith("www.")) {
            host = host.drop(4)
        }
        return host
    }

    /**
     * Mirrors `sitename(value)` in `../news/app/filters.py` and `NewsFormatter.siteName` in `../macnews`.
     */
    fun siteName(fromDomainOrUrl: String): String {
        val host = domainName(fromDomainOrUrl)
        val parts = host.split(".").filter { it.isNotEmpty() }
        if (parts.size < 2) {
            return host
        }
        if (parts.size >= 3) {
            val lastTwo = "${parts[parts.size - 2]}.${parts[parts.size - 1]}".lowercase()
            if (TWO_PART_TLDS.contains(lastTwo)) {
                return parts.dropLast(2).joinToString(".")
            }
        }
        return parts.dropLast(1).joinToString(".")
    }

    /**
     * Formats the active site/domain button label in the bottom navigation bar.
     */
    fun siteNavigationLabel(site: String): String = site.trim()

    /**
     * Returns the bottom navigation options when a site/domain is selected:
     * the site/domain name option followed by the multiply sign (`×`) close option.
     */
    fun siteNavigationOptions(site: String): List<String> =
        listOf(siteNavigationLabel(site), MULTIPLY_SIGN)
}

@Serializable
data class NewsEntry(
    val id: String,
    val url: String,
    val title: String,
    val domain: String = "",
    val site: String = "",
    val pub: Double = 0.0,
    val age: String = "",
    val author: String = "",
    val description: String = "",
    val summary: String = "",
    val score: Double = 0.0,
    val degrees: Int? = null
) {
    val siteLabel: String
        get() = site.ifBlank {
            NewsFormatter.siteName(domain.ifBlank { url })
        }

    val domainLabel: String
        get() = NewsFormatter.domainName(domain.ifBlank { url })

    val resolvedAge: String
        get() = age.ifBlank { NewsFormatter.shortDate(pub) }

    val ageLabel: String
        get() = if (resolvedAge.endsWith("ago")) resolvedAge else "$resolvedAge ago"

    val resolvedSummary: String
        get() = summary.ifBlank { NewsFormatter.truncateSummary(description) }

    val resolvedDegrees: Int
        get() = degrees ?: (score * 1000.0).roundToInt()

    val degreesLabel: String
        get() = "${resolvedDegrees}°"
}

@Serializable
data class PaginatedNewsResponse(
    val page: Int = 1,
    val pages: Int = 0,
    val view: String = "hottest",
    val site: String? = null,
    val entries: List<NewsEntry> = emptyList()
) {
    val resolvedPages: Int
        get() = when {
            pages > 0 -> pages
            entries.isEmpty() -> 0
            else -> page
        }

    val hasNextPage: Boolean
        get() = page < resolvedPages && entries.isNotEmpty()
}
