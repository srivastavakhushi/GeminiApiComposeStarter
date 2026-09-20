package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel(
    private val repository: GeminiRepository,
    hasApiKey: Boolean,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ChatUiState(
            configurationError = if (hasApiKey) null else MISSING_API_KEY_MESSAGE,
        )
    )
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    /** Prompt behind the last failed request, so the snackbar action can resend it. */
    private var lastFailedPrompt: String? = null

    fun onPromptChange(value: String) {
        _uiState.update {
            it.copy(
                prompt = value,
                promptError = null,
            )
        }
    }

    fun onSend() {
        val state = _uiState.value
        val prompt = state.prompt.trim()

        if (prompt.isEmpty()) {
            _uiState.update { it.copy(promptError = PromptError.EMPTY) }
            return
        }

        if (!state.canSend) return

        val userMessage = ChatMessage(
            id = newMessageId(),
            text = prompt,
            isUser = true,
        )

        _uiState.update {
            it.copy(
                messages = it.messages + userMessage,
                prompt = "",
                isLoading = true,
                errorMessage = null,
                promptError = null,
            )
        }

        generate(prompt)
    }

    /** Resends the prompt that failed, without adding a second copy of the user's message. */
    fun onRetry() {
        val prompt = lastFailedPrompt ?: return
        if (!_uiState.value.canSend) return

        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessage = null,
            )
        }

        generate(prompt)
    }

    /** Called once the snackbar has shown the message so it is not displayed twice. */
    fun onErrorShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun generate(prompt: String) {
        viewModelScope.launch {
            repository.generateText(prompt).fold(
                onSuccess = { text ->
                    lastFailedPrompt = null

                    val geminiMessage = ChatMessage(
                        id = newMessageId(),
                        text = text,
                        isUser = false,
                    )

                    _uiState.update {
                        it.copy(
                            messages = it.messages + geminiMessage,
                            isLoading = false,
                        )
                    }
                },

                onFailure = { error ->
                    lastFailedPrompt = prompt

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: GENERIC_ERROR_MESSAGE,
                        )
                    }
                },
            )
        }
    }

    private fun newMessageId(): String = UUID.randomUUID().toString()

    companion object {

        const val MISSING_API_KEY_MESSAGE =
            "GEMINI_API_KEY is missing. Add it to local.properties and rebuild."

        const val GENERIC_ERROR_MESSAGE = "Something went wrong. Please try again."

        fun factory(
            repository: GeminiRepository,
            hasApiKey: Boolean,
        ) = object : ViewModelProvider.Factory {

            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>
            ): T =
                ChatViewModel(
                    repository,
                    hasApiKey
                ) as T
        }
    }
}
