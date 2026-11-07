package com.lucianmarin.news

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.lucianmarin.news.state.AppStateViewModel
import com.lucianmarin.news.ui.navigation.MainScaffold
import com.lucianmarin.news.ui.theme.HotnewsTheme

class MainActivity : ComponentActivity() {
    private val viewModel: AppStateViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        viewModel.handleDeepLink(intent)

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            HotnewsTheme(themeMode = uiState.themeMode) {
                MainScaffold(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        viewModel.handleDeepLink(intent)
    }
}
