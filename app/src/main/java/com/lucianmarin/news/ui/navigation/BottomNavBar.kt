package com.lucianmarin.news.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.AccessTimeFilled
import androidx.compose.material.icons.rounded.HighlightOff
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.StickyNote2
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucianmarin.news.model.FeedTab
import com.lucianmarin.news.model.NewsFormatter
import com.lucianmarin.news.state.AppUiState
import com.lucianmarin.news.ui.theme.NewsTheme
import com.lucianmarin.news.ui.theme.Route159FontFamily

/**
 * Floating bottom navigation bar styled after `../subdroid`'s `SidebarView`:
 * - Default mode: `Hottest`, `Coldest`, `Newest` (showing a native Material Rounded icon when active).
 * - Site/domain mode (`selectedSite != null`): displays the selected `<site>` option with a
 *   sticky-note icon alongside a `HighlightOffRounded` close icon button in the nav bar for closing
 *   the filter and returning to the `Hottest` section.
 */
val FeedTab.activeIcon: ImageVector
    get() = when (this) {
        FeedTab.HOT -> Icons.Rounded.LocalFireDepartment
        FeedTab.COLD -> Icons.Rounded.AcUnit
        FeedTab.NEW -> Icons.Rounded.AccessTimeFilled
    }

val ActiveSiteNavIcon: ImageVector
    get() = Icons.Rounded.StickyNote2

val CloseSiteNavIcon: ImageVector
    get() = Icons.Rounded.HighlightOff

@Composable
fun BottomNavBar(
    uiState: AppUiState,
    onSelectTab: (FeedTab) -> Unit,
    onSelectSite: (String) -> Unit,
    onClearSite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeSite = uiState.selectedSite?.takeIf { it.isNotBlank() }

    Row(
        modifier = modifier
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(25.dp))
            .clip(RoundedCornerShape(10.dp))
            .background(NewsTheme.floatingBarBackground)
            .padding(horizontal = 5.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (activeSite != null) {
            val siteLabel = NewsFormatter.siteNavigationLabel(activeSite)
            BottomNavTextButton(
                title = siteLabel,
                contentDesc = siteLabel,
                isSelected = true,
                activeIcon = ActiveSiteNavIcon,
                onClick = { onSelectSite(siteLabel) }
            )
            BottomNavIconButton(
                icon = CloseSiteNavIcon,
                contentDesc = "Close $siteLabel and return to Hottest",
                onClick = onClearSite
            )
        } else {
            for (tab in FeedTab.entries) {
                BottomNavTextButton(
                    title = tab.title,
                    contentDesc = tab.title,
                    isSelected = uiState.selectedTab == tab,
                    activeIcon = tab.activeIcon,
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
    onClick: () -> Unit,
    activeIcon: ImageVector? = null
) {
    val bgColor = if (isSelected) NewsTheme.accent else Color.Transparent
    val textColor = if (isSelected) NewsTheme.background else NewsTheme.gray

    Box(
        modifier = Modifier
            .height(35.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .semantics { contentDescription = contentDesc }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 15.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelected && activeIcon != null) {
                Icon(
                    imageVector = activeIcon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = title,
                fontFamily = Route159FontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                lineHeight = 25.sp,
                color = textColor
            )
        }
    }
}

@Composable
private fun BottomNavIconButton(
    icon: ImageVector,
    contentDesc: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(35.dp)
            .clip(RoundedCornerShape(20.dp))
            .semantics { contentDescription = contentDesc }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = NewsTheme.gray,
            modifier = Modifier.size(16.dp)
        )
    }
}
