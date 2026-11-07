package com.lucianmarin.news.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucianmarin.news.model.NewsEntry
import com.lucianmarin.news.state.NewsStateController
import com.lucianmarin.news.ui.theme.NewsDivider
import com.lucianmarin.news.ui.theme.NewsTheme
import com.lucianmarin.news.ui.theme.Route159FontFamily

/**
 * Mirrors `NewsRowView.swift` in `../macnews` and `templates/entry.html` in `../news`:
 * - Alternating odd/even row highlight (`#F0F0F0` / `#141414` on odd rows, `#FFFFFF` / `#000000` on even rows)
 * - `.small` metadata line: clickable `site` + `age ago` + `score°` in `#FF4500` (`orangered`)
 * - Bold `14.sp` clickable headline with visited state (`--darkgray`)
 * - Regular `14.sp` truncated summary (`--darkgray`, up to 480 chars)
 * - `1.dp` `--gainsboro` bottom divider
 */
@Composable
fun NewsRowView(
    entry: NewsEntry,
    rowIndex: Int,
    isVisited: Boolean,
    onSelectSite: (String) -> Unit,
    onOpenArticle: (NewsEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    val rowBg = NewsTheme.rowBackground(forZeroBasedIndex = rowIndex)
    val summary = entry.resolvedSummary

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
                // .small metadata row: site + age ago + degrees°
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = entry.siteLabel,
                        fontFamily = Route159FontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 13.sp,
                        lineHeight = 16.sp,
                        color = NewsTheme.primaryText,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onSelectSite(entry.siteLabel) }
                        )
                    )

                    Text(
                        text = entry.ageLabel,
                        fontFamily = Route159FontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 13.sp,
                        lineHeight = 16.sp,
                        color = NewsTheme.gray
                    )

                    Text(
                        text = entry.degreesLabel,
                        fontFamily = Route159FontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 13.sp,
                        lineHeight = 16.sp,
                        color = NewsTheme.accent
                    )

                    Spacer(modifier = Modifier.weight(1f))
                }

                // h4.title headline
                Text(
                    text = entry.title,
                    fontFamily = Route159FontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = if (isVisited) NewsTheme.visitedText else NewsTheme.primaryText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onOpenArticle(entry) }
                        )
                )

                // .summary truncated description
                if (summary.isNotEmpty()) {
                    Text(
                        text = summary,
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

        NewsDivider()
    }
}

fun isOddNewsRow(zeroBasedIndex: Int): Boolean =
    NewsStateController.isOddRowHighlighted(zeroBasedIndex)
