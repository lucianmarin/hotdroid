package com.lucianmarin.news.ui.navigation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.lucianmarin.news.state.AppStateViewModel
import com.lucianmarin.news.ui.screens.AboutScreen
import com.lucianmarin.news.ui.screens.ArticleWebViewScreen
import com.lucianmarin.news.ui.screens.FeedScreen
import com.lucianmarin.news.ui.theme.NewsTheme

/**
 * Main application scaffold with full-width alternating row backgrounds,
 * a centered 520dp reading column (560dp with horizontal padding),
 * an in-app About page and article WebView, and a floating bottom navigation bar pill.
 */
@Composable
fun MainScaffold(
    viewModel: AppStateViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    BackHandler(enabled = uiState.canNavigateBack && !uiState.isWebPageOpen) {
        viewModel.navigateBack()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NewsTheme.background)
    ) {
        if (uiState.isAboutOpen) {
            AboutScreen(
                publications = uiState.publications,
                onSelectSite = { site -> viewModel.selectSite(site) },
                onOpenUrl = { url ->
                    val trimmed = url.trim()
                    if (trimmed.startsWith("mailto:", ignoreCase = true) ||
                        trimmed.startsWith("tel:", ignoreCase = true)
                    ) {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(trimmed)))
                        }
                    } else {
                        viewModel.openWebUrl(trimmed)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            FeedScreen(
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize()
            )
        }

        val retainedWebUrl = uiState.retainedWebUrl
        if (!retainedWebUrl.isNullOrBlank()) {
            ArticleWebViewScreen(
                url = retainedWebUrl,
                isVisible = uiState.isWebPageOpen,
                onUrlChanged = { newUrl -> viewModel.updateActiveWebUrl(newUrl) },
                onClose = { viewModel.closeWebPage() },
                modifier = Modifier.fillMaxSize()
            )
        }

        BottomNavBar(
            uiState = uiState,
            onSelectTab = { viewModel.selectTab(it) },
            onSelectSite = { viewModel.selectSite(it) },
            onClearSite = { viewModel.clearSiteFilter() },
            onOpenAbout = { viewModel.openAboutPage() },
            onCloseAbout = { viewModel.closeAboutPage() },
            onSelectWebDomain = { viewModel.selectSiteFromWebPage() },
            onCopyWebUrl = { url -> copyUrlToClipboard(context, url) },
            onCloseWebPage = { viewModel.closeWebPage() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 15.dp)
        )
    }
}

fun copyUrlToClipboard(context: Context, url: String) {
    val cleanUrl = url.trim()
    if (cleanUrl.isEmpty()) return
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText("URL", cleanUrl))
}
