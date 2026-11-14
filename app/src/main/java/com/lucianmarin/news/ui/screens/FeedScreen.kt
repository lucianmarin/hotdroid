package com.lucianmarin.news.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.lucianmarin.news.state.AppStateViewModel
import com.lucianmarin.news.ui.theme.NewsTheme
import com.lucianmarin.news.ui.theme.Route159FontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Main news stream screen with Material3 pull-to-refresh and infinite scroll pagination.
 * Mirrors `MainNewsView.swift` in `../macnews` and `FeedStreamScreen` in `../subdroid`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    viewModel: AppStateViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val pullState = rememberPullToRefreshState()
    var isPullRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.scrollResetVersion) {
        if (uiState.scrollResetVersion > 0) {
            listState.scrollToItem(0)
        }
    }

    PullToRefreshBox(
        isRefreshing = isPullRefreshing,
        onRefresh = {
            if (!isPullRefreshing) {
                isPullRefreshing = true
                scope.launch {
                    val startMs = System.currentTimeMillis()
                    try {
                        viewModel.loadFeed(reset = true)
                    } finally {
                        val elapsedMs = System.currentTimeMillis() - startMs
                        if (elapsedMs < 350L) {
                            delay(350L - elapsedMs)
                        }
                        isPullRefreshing = false
                    }
                }
            }
        },
        state = pullState,
        modifier = modifier
            .fillMaxSize()
            .background(NewsTheme.lastRowBackground(entryCount = uiState.entries.size)),
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullState,
                isRefreshing = isPullRefreshing,
                containerColor = NewsTheme.oddRowBackground,
                color = NewsTheme.accent,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
            )
        }
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
        ) {
            item(key = "top_status_bar_spacer") {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NewsTheme.oddRowBackground)
                        .statusBarsPadding()
                )
            }

            if (uiState.isLoading && uiState.entries.isEmpty()) {
                item(key = "status_loading") {
                    FeedStatusRow {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = NewsTheme.accent
                            )
                            Text(
                                text = "Fetching articles...",
                                fontFamily = Route159FontFamily,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                color = NewsTheme.gray
                            )
                        }
                    }
                }
            } else if (uiState.errorMessage != null && uiState.entries.isEmpty()) {
                item(key = "status_error") {
                    FeedStatusRow {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = uiState.errorMessage ?: "Unable to load news feed",
                                fontFamily = Route159FontFamily,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                color = NewsTheme.accent
                            )
                            Text(
                                text = "Retry",
                                fontFamily = Route159FontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                lineHeight = 20.sp,
                                color = NewsTheme.primaryText,
                                modifier = Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { viewModel.refresh() }
                                )
                            )
                        }
                    }
                }
            } else if (uiState.entries.isEmpty()) {
                item(key = "status_empty") {
                    FeedStatusRow {
                        Text(
                            text = "Fetching articles...",
                            fontFamily = Route159FontFamily,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = NewsTheme.gray
                        )
                    }
                }
            } else {
                itemsIndexed(
                    items = uiState.entries,
                    key = { _, entry -> entry.id }
                ) { index, entry ->
                    LaunchedEffect(entry.id, uiState.entries.size, uiState.currentPage, uiState.totalPages) {
                        viewModel.loadMoreIfNeeded(entry)
                    }

                    NewsRowView(
                        entry = entry,
                        rowIndex = index,
                        isVisited = uiState.visitedUrls.contains(entry.url),
                        onSelectSite = { site -> viewModel.selectSite(site) },
                        onOpenArticle = { clickedEntry ->
                            viewModel.openArticle(clickedEntry)
                        },
                        showBottomDivider = shouldShowBottomDivider(
                            zeroBasedIndex = index,
                            totalCount = uiState.entries.size
                        )
                    )
                }

                if (uiState.isLoadingMore) {
                    item(key = "loading_more") {
                        val lastRowBg = NewsTheme.lastRowBackground(entryCount = uiState.entries.size)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(lastRowBg)
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = NewsTheme.accent
                                )
                                Text(
                                    text = "Loading more...",
                                    fontFamily = Route159FontFamily,
                                    fontSize = 13.sp,
                                    lineHeight = 20.sp,
                                    color = NewsTheme.gray
                                )
                            }
                        }
                    }
                } else if (uiState.canLoadMore) {
                    item(key = "load_more") {
                        val lastRowBg = NewsTheme.lastRowBackground(entryCount = uiState.entries.size)
                        LaunchedEffect(uiState.currentPage, uiState.entries.size) {
                            if (!uiState.loadMoreError) {
                                viewModel.loadFeed(reset = false)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(lastRowBg)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {
                                        scope.launch {
                                            viewModel.loadFeed(reset = false)
                                        }
                                    }
                                )
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (uiState.loadMoreError) "Retry" else "Load more",
                                fontFamily = Route159FontFamily,
                                fontSize = 13.sp,
                                lineHeight = 20.sp,
                                color = if (uiState.loadMoreError) NewsTheme.accent else NewsTheme.gray
                            )
                        }
                    }
                }
            }

            item(key = "bottom_floating_bar_spacer") {
                val lastRowBg = NewsTheme.lastRowBackground(entryCount = uiState.entries.size)
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
}

@Composable
private fun FeedStatusRow(
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(NewsTheme.oddRowBackground)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = NewsTheme.maxColumnWidth)
                    .fillMaxWidth()
                    .padding(
                        horizontal = NewsTheme.horizontalPadding,
                        vertical = 16.dp
                    )
            ) {
                content()
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ArticleWebViewScreen(
    url: String,
    isVisible: Boolean = true,
    onUrlChanged: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var lastLoadedInputUrl by remember { mutableStateOf<String?>(null) }
    val bgColorArgb = NewsTheme.background.toArgb()

    BackHandler(enabled = isVisible) {
        val currentWebView = webViewRef
        if (currentWebView != null && currentWebView.canGoBack()) {
            currentWebView.goBack()
        } else {
            onClose()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.stopLoading()
            webViewRef?.destroy()
            webViewRef = null
        }
    }

    Column(
        modifier = if (isVisible) {
            modifier
                .fillMaxSize()
                .background(NewsTheme.background)
        } else {
            Modifier.size(0.dp)
        }
    ) {
        if (isVisible) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NewsTheme.oddRowBackground)
                    .statusBarsPadding()
            )
        }

        Box(modifier = if (isVisible) Modifier.fillMaxSize() else Modifier.size(0.dp)) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        setBackgroundColor(bgColorArgb)
                        visibility = if (isVisible) android.view.View.VISIBLE else android.view.View.GONE
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            builtInZoomControls = true
                            displayZoomControls = false
                            setSupportZoom(true)
                        }
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, startedUrl: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, startedUrl, favicon)
                                isLoading = true
                                if (!startedUrl.isNullOrBlank()) {
                                    onUrlChanged(startedUrl)
                                }
                            }

                            override fun onPageFinished(view: WebView?, finishedUrl: String?) {
                                super.onPageFinished(view, finishedUrl)
                                isLoading = false
                                if (!finishedUrl.isNullOrBlank()) {
                                    onUrlChanged(finishedUrl)
                                }
                            }

                            override fun doUpdateVisitedHistory(view: WebView?, visitedUrl: String?, isReload: Boolean) {
                                super.doUpdateVisitedHistory(view, visitedUrl, isReload)
                                if (!visitedUrl.isNullOrBlank()) {
                                    onUrlChanged(visitedUrl)
                                }
                            }

                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val targetUri = request?.url ?: return false
                                val scheme = targetUri.scheme?.lowercase()
                                if (scheme == "http" || scheme == "https") {
                                    return false
                                }
                                return try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, targetUri))
                                    true
                                } catch (_: Exception) {
                                    true
                                }
                            }
                        }
                        webViewRef = this
                        lastLoadedInputUrl = url
                        loadUrl(url)
                    }
                },
                update = { webView ->
                    webView.setBackgroundColor(bgColorArgb)
                    webView.visibility = if (isVisible) android.view.View.VISIBLE else android.view.View.GONE
                    if (url != lastLoadedInputUrl && url != webView.url) {
                        lastLoadedInputUrl = url
                        webView.loadUrl(url)
                    }
                },
                modifier = if (isVisible) Modifier.fillMaxSize() else Modifier.size(0.dp)
            )

            if (isVisible && isLoading) {
                LinearProgressIndicator(
                    color = NewsTheme.accent,
                    trackColor = NewsTheme.oddRowBackground,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .align(Alignment.TopCenter)
                )
            }
        }
    }
}
