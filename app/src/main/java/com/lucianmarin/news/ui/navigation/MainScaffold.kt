package com.lucianmarin.news.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lucianmarin.news.state.AppStateViewModel
import com.lucianmarin.news.ui.screens.FeedScreen
import com.lucianmarin.news.ui.theme.NewsTheme

/**
 * Main application scaffold with full-width alternating row backgrounds,
 * a centered 520dp reading column (560dp with horizontal padding),
 * and a floating text-only bottom navigation bar matching `../subdroid`.
 */
@Composable
fun MainScaffold(
    viewModel: AppStateViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    BackHandler(enabled = uiState.isSiteSelected) {
        viewModel.clearSiteFilter()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NewsTheme.background)
            .statusBarsPadding()
    ) {
        FeedScreen(
            viewModel = viewModel,
            modifier = Modifier.fillMaxSize()
        )

        BottomNavBar(
            uiState = uiState,
            onSelectTab = { viewModel.selectTab(it) },
            onSelectSite = { viewModel.selectSite(it) },
            onClearSite = { viewModel.clearSiteFilter() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 15.dp)
        )
    }
}
