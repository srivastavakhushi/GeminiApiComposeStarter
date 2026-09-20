package com.fahim.geminiApiComposeStarter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.security.ApiKeyManager
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by viewModels {

        val apiKeyManager =
            ApiKeyManager(applicationContext)

        var apiKey = apiKeyManager.getApiKey()
        val configuredApiKey = BuildConfig.GEMINI_API_KEY.trim()

        // Refresh the encrypted value when local.properties changes.
        if (configuredApiKey.isNotEmpty() && apiKey != configuredApiKey) {
            apiKeyManager.saveApiKey(configuredApiKey)
            apiKey = apiKeyManager.getApiKey()
        }

        ChatViewModel.factory(
            repository = GeminiRepositoryImpl(
                apiKey = apiKey ?: ""
            ),
            hasApiKey = !apiKey.isNullOrBlank()
        )
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            GeminiApiComposeStarterTheme {
                ChatRoute(
                    viewModel = viewModel,
                    windowSizeClass = calculateWindowSizeClass(this),
                )
            }
        }
    }
}