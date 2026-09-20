package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.runtime.Immutable

@Immutable
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val prompt: String = "",
    val isLoading: Boolean = false,
    val promptError: PromptError? = null,
    /** Transient failure shown in the snackbar and cleared once it has been displayed. */
    val errorMessage: String? = null,
    /** Setup problem the user has to fix before chatting; stays on screen as a card. */
    val configurationError: String? = null,
) {
    val canSend: Boolean get() = !isLoading && configurationError == null
}

@Immutable
data class ChatMessage(
    val id: String,
    val text: String,
    val isUser: Boolean,
)

enum class PromptError { EMPTY }
