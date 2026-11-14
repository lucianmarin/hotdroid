package com.lucianmarin.news.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucianmarin.news.ui.theme.NewsDivider
import com.lucianmarin.news.ui.theme.NewsTheme
import com.lucianmarin.news.ui.theme.Route159FontFamily

data class AboutTextSegment(
    val text: String,
    val url: String? = null,
    val isNameLink: Boolean = false
)

data class AboutSection(
    val id: String,
    val title: String,
    val paragraphs: List<List<AboutTextSegment>>
) {
    val plainTextParagraphs: List<String>
        get() = paragraphs.map { segments -> segments.joinToString("") { it.text } }
}

object AboutContent {
    const val BRAND_NAME = "Hotnews"

    val DEFAULT_PUBLICATIONS: List<String> = emptyList()

    fun buildSections(
        publications: List<String> = DEFAULT_PUBLICATIONS
    ): List<AboutSection> {
        val cleanSites = publications
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .sorted()
        val siteCount = cleanSites.size

        val publicationSegments = if (cleanSites.isEmpty()) {
            listOf(AboutTextSegment(text = "Fetching publications..."))
        } else {
            buildList {
                cleanSites.forEachIndexed { index, site ->
                    add(AboutTextSegment(text = site, url = "/$site"))
                    if (index < cleanSites.lastIndex) {
                        add(AboutTextSegment(text = ", "))
                    } else {
                        add(AboutTextSegment(text = "."))
                    }
                }
            }
        }

        return listOf(
            AboutSection(
                id = "intro",
                title = "About $BRAND_NAME",
                paragraphs = listOf(
                    listOf(
                        AboutTextSegment(
                            text = "Welcome to a minimalist news aggregator built for signal over noise. By stripping away engagement bait, comment sections and visual clutter, $BRAND_NAME lets you focus on what actually matters: the stories shaping the day."
                        )
                    ),
                    listOf(
                        AboutTextSegment(text = BRAND_NAME, url = "/", isNameLink = true),
                        AboutTextSegment(
                            text = " aggregates articles from a curated list of RSS feeds, scores them based on title and description similarity across publications, and surfaces the top story from each domain in a calm, distraction-free interface."
                        )
                    ),
                    listOf(
                        AboutTextSegment(
                            text = "Lucian Marin",
                            url = "https://lucianmarin.com/",
                            isNameLink = true
                        ),
                        AboutTextSegment(
                            text = " created $BRAND_NAME for busy people who don't want to spend more than a few minutes getting the most important news of the day without algorithmic manipulation or endless doomscrolling."
                        )
                    )
                )
            ),
            AboutSection(
                id = "tech_stack",
                title = "Tech stack and support",
                paragraphs = listOf(
                    listOf(
                        AboutTextSegment(text = "1. Hosted on a high-frequency "),
                        AboutTextSegment(
                            text = "Vultr VPS",
                            url = "https://vultr.com/?ref=8946799-8H"
                        ),
                        AboutTextSegment(text = " running Ubuntu 24.04 LTS.")
                    ),
                    listOf(
                        AboutTextSegment(text = "2. Built with Python 3.12, "),
                        AboutTextSegment(
                            text = "FastAPI",
                            url = "https://fastapi.tiangolo.com/"
                        ),
                        AboutTextSegment(text = ", "),
                        AboutTextSegment(
                            text = "Jinja2",
                            url = "https://jinja.palletsprojects.com/"
                        ),
                        AboutTextSegment(text = " and Tortoise ORM.")
                    ),
                    listOf(
                        AboutTextSegment(
                            text = "3. Powered by Uvicorn behind Nginx and backed by SQLite with aiosqlite."
                        )
                    ),
                    listOf(
                        AboutTextSegment(
                            text = "4. RSS aggregation with aiohttp, feedparser and BeautifulSoup; similarity scoring with scikit-learn and SciPy."
                        )
                    ),
                    listOf(
                        AboutTextSegment(text = "5. Icons by "),
                        AboutTextSegment(
                            text = "Heroicons",
                            url = "https://heroicons.com/"
                        ),
                        AboutTextSegment(text = " and "),
                        AboutTextSegment(
                            text = "Sidekickicons",
                            url = "https://sidekickicons.com/"
                        ),
                        AboutTextSegment(text = ", Route 159 typeface by "),
                        AboutTextSegment(
                            text = "Sora Sagano",
                            url = "https://dotcolon.net/fonts/route159"
                        ),
                        AboutTextSegment(text = ".")
                    ),
                    listOf(
                        AboutTextSegment(text = "6. "),
                        AboutTextSegment(
                            text = "Donate via PayPal",
                            url = "https://paypal.com/donate/?hosted_button_id=586UTPPSHBWAN"
                        ),
                        AboutTextSegment(text = " to keep $BRAND_NAME ad-free and running indefinitely.")
                    )
                )
            ),
            AboutSection(
                id = "features",
                title = "Features and design constraints",
                paragraphs = listOf(
                    listOf(
                        AboutTextSegment(
                            text = if (siteCount > 0) {
                                "1. Curated RSS aggregation across $siteCount tech, science, culture and news publications."
                            } else {
                                "1. Curated RSS aggregation across tech, science, culture and news publications."
                            }
                        )
                    ),
                    listOf(
                        AboutTextSegment(
                            text = "2. TF-IDF vectorization and cosine similarity scoring across article titles and descriptions."
                        )
                    ),
                    listOf(
                        AboutTextSegment(
                            text = "3. Hottest view ranks stories covered across multiple publications by highest similarity score."
                        )
                    ),
                    listOf(
                        AboutTextSegment(
                            text = "4. Coldest view highlights unique stories with the lowest similarity score across publications."
                        )
                    ),
                    listOf(
                        AboutTextSegment(
                            text = "5. Newest view presents the latest stories in reverse-chronological order."
                        )
                    ),
                    listOf(
                        AboutTextSegment(
                            text = "6. One top story per domain in main feeds so no single publication dominates the page."
                        )
                    ),
                    listOf(
                        AboutTextSegment(
                            text = "7. Dedicated per-publication feeds showing all recent stories from a specific source."
                        )
                    ),
                    listOf(
                        AboutTextSegment(
                            text = "8. Automatic cleanup of articles older than 48 hours to keep every feed fresh."
                        )
                    ),
                    listOf(
                        AboutTextSegment(
                            text = "9. Concise summaries truncated to 480 characters with temperature scores in degrees."
                        )
                    ),
                    listOf(
                        AboutTextSegment(
                            text = "10. Automatic light and dark themes following your OS preference."
                        )
                    )
                )
            ),
            AboutSection(
                id = "publications",
                title = if (siteCount > 0) "$siteCount publications" else "Publications",
                paragraphs = listOf(publicationSegments)
            ),
            AboutSection(
                id = "contact_and_privacy",
                title = "Contact and privacy",
                paragraphs = listOf(
                    listOf(
                        AboutTextSegment(
                            text = "Respecting readers' privacy is important to $BRAND_NAME. Under our "
                        ),
                        AboutTextSegment(
                            text = "privacy policy",
                            url = "https://news.lucianmarin.com/privacy"
                        ),
                        AboutTextSegment(
                            text = ", no registration, user accounts, tracking cookies, third-party analytics or advertising trackers are used. Articles and metadata are fetched from publicly available RSS feeds, cached for 48 hours and then permanently erased, while clicking a headline takes you directly to the publisher's original website."
                        )
                    ),
                    listOf(
                        AboutTextSegment(text = "Gmail"),
                        AboutTextSegment(text = " — "),
                        AboutTextSegment(
                            text = "marin.lucian",
                            url = "mailto:marin.lucian@gmail.com"
                        )
                    ),
                    listOf(
                        AboutTextSegment(text = "Phone"),
                        AboutTextSegment(text = " — "),
                        AboutTextSegment(
                            text = "+40 726 210 589",
                            url = "tel:+40726210589"
                        )
                    )
                )
            )
        )
    }
}

/**
 * In-app About page mirroring `../news/templates/about.html`, where each section is
 * styled identically to an article row in `NewsRowView`.
 */
@Composable
fun AboutScreen(
    publications: List<String> = AboutContent.DEFAULT_PUBLICATIONS,
    onSelectSite: (String) -> Unit,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val sections = remember(publications) {
        AboutContent.buildSections(publications)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(NewsTheme.lastRowBackground(entryCount = sections.size))
    ) {
        item(key = "about_top_status_bar_spacer") {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NewsTheme.oddRowBackground)
                    .statusBarsPadding()
            )
        }

        itemsIndexed(
            items = sections,
            key = { _, section -> section.id }
        ) { index, section ->
            AboutSectionRowView(
                section = section,
                rowIndex = index,
                onSelectSite = onSelectSite,
                onOpenUrl = onOpenUrl,
                showBottomDivider = shouldShowBottomDivider(
                    zeroBasedIndex = index,
                    totalCount = sections.size
                )
            )
        }

        item(key = "about_bottom_floating_bar_spacer") {
            val lastRowBg = NewsTheme.lastRowBackground(entryCount = sections.size)
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(lastRowBg)
                    .navigationBarsPadding()
                    .height(72.dp)
            )
        }
    }
}

@Composable
fun AboutSectionRowView(
    section: AboutSection,
    rowIndex: Int,
    onSelectSite: (String) -> Unit,
    onOpenUrl: (String) -> Unit,
    showBottomDivider: Boolean = true,
    modifier: Modifier = Modifier
) {
    val rowBg = NewsTheme.rowBackground(forZeroBasedIndex = rowIndex)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(rowBg)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = NewsTheme.maxColumnWidth)
                    .fillMaxWidth()
                    .padding(
                        horizontal = NewsTheme.horizontalPadding,
                        vertical = NewsTheme.rowVerticalPadding
                    ),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = section.title,
                    fontFamily = Route159FontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = NewsTheme.primaryText,
                    modifier = Modifier.fillMaxWidth()
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    section.paragraphs.forEachIndexed { paragraphIndex, segments ->
                        val annotated = buildAboutParagraphAnnotatedString(
                            segments = segments,
                            sectionId = section.id,
                            paragraphIndex = paragraphIndex,
                            onSelectSite = onSelectSite,
                            onOpenUrl = onOpenUrl
                        )
                        Text(
                            text = annotated,
                            fontFamily = Route159FontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = NewsTheme.darkGray,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        if (showBottomDivider) {
            NewsDivider()
        }
    }
}

@Composable
private fun buildAboutParagraphAnnotatedString(
    segments: List<AboutTextSegment>,
    sectionId: String,
    paragraphIndex: Int,
    onSelectSite: (String) -> Unit,
    onOpenUrl: (String) -> Unit
): AnnotatedString {
    val primaryColor = NewsTheme.primaryText
    val darkGrayColor = NewsTheme.darkGray
    val accentColor = NewsTheme.accent

    return buildAnnotatedString {
        segments.forEachIndexed { segmentIndex, segment ->
            val targetUrl = segment.url
            if (targetUrl != null) {
                val linkStyle = TextLinkStyles(
                    style = SpanStyle(
                        fontFamily = Route159FontFamily,
                        fontWeight = if (segment.isNameLink) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 14.sp,
                        color = if (segment.isNameLink) primaryColor else accentColor
                    )
                )
                withLink(
                    LinkAnnotation.Clickable(
                        tag = "about_${sectionId}_${paragraphIndex}_$segmentIndex",
                        styles = linkStyle,
                        linkInteractionListener = {
                            if (targetUrl.startsWith("/") && targetUrl.length > 1) {
                                onSelectSite(targetUrl.removePrefix("/"))
                            } else {
                                onOpenUrl(targetUrl)
                            }
                        }
                    )
                ) {
                    append(segment.text)
                }
            } else {
                withStyle(
                    SpanStyle(
                        fontFamily = Route159FontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 14.sp,
                        color = darkGrayColor
                    )
                ) {
                    append(segment.text)
                }
            }
        }
    }
}
