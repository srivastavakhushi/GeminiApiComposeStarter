package com.fahim.geminiApiComposeStarter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.security.ApiKeyManager
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by viewModels {

        val apiKeyManager =
            ApiKeyManager(applicationContext)

        // Get encrypted API key if it already exists
        var apiKey = apiKeyManager.getApiKey()

        // First launch: encrypt the API key from BuildConfig
        if (apiKey == null && BuildConfig.GEMINI_API_KEY.isNotBlank()) {

            apiKeyManager.saveApiKey(
                BuildConfig.GEMINI_API_KEY
            )

            apiKey = apiKeyManager.getApiKey()
        }

        ChatViewModel.factory(
            repository = GeminiRepositoryImpl(
                apiKey = apiKey ?: ""
            ),
            hasApiKey = !apiKey.isNullOrBlank()
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            GeminiApiComposeStarterTheme {
                ChatRoute(
                    viewModel = viewModel
                )
            }
        }
    }
}