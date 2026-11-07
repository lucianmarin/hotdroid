package com.lucianmarin.news.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucianmarin.news.model.FeedTab
import com.lucianmarin.news.model.NewsFormatter
import com.lucianmarin.news.state.AppUiState
import com.lucianmarin.news.ui.theme.LocalNewsPalette
import com.lucianmarin.news.ui.theme.NewsTheme
import com.lucianmarin.news.ui.theme.Route159FontFamily

/**
 * Floating bottom navigation bar styled after `../subdroid`'s `SidebarView`,
 * using text-only navigation buttons:
 * - Default mode: `Hottest`, `Coldest`, `Newest`.
 * - Site/domain mode (`selectedSite != null`): displays the selected `<site>` option alongside
 *   a separate multiply sign (`×`) option/icon in the nav bar for closing the filter
 *   and returning to the `Hottest` section.
 */
@Composable
fun BottomNavBar(
    uiState: AppUiState,
    onSelectTab: (FeedTab) -> Unit,
    onSelectSite: (String) -> Unit,
    onClearSite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalNewsPalette.current.isDark
    val barBg = if (isDark) NewsTheme.floatingBarBackground else NewsTheme.background
    val activeSite = uiState.selectedSite?.takeIf { it.isNotBlank() }

    Row(
        modifier = modifier
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .background(barBg)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (activeSite != null) {
            val siteLabel = NewsFormatter.siteNavigationLabel(activeSite)
            BottomNavTextButton(
                title = siteLabel,
                contentDesc = siteLabel,
                isSelected = true,
                onClick = { onSelectSite(siteLabel) }
            )
            BottomNavTextButton(
                title = NewsFormatter.MULTIPLY_SIGN,
                contentDesc = "Close $siteLabel and return to Hottest",
                isSelected = false,
                onClick = onClearSite
            )
        } else {
            for (tab in FeedTab.entries) {
                BottomNavTextButton(
                    title = tab.title,
                    contentDesc = tab.title,
                    isSelected = uiState.selectedTab == tab,
                    onClick = { onSelectTab(tab) }
                )
            }
        }
    }
}

@Composable
private fun BottomNavTextButton(
    title: String,
    contentDesc: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) NewsTheme.accent else Color.Transparent
    val textColor = if (isSelected) NewsTheme.background else NewsTheme.gray

    Box(
        modifier = Modifier
            .height(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .semantics { contentDescription = contentDesc }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            fontFamily = Route159FontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = textColor
        )
    }
}
