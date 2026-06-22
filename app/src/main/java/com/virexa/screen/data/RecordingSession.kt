package com.virexa.screen.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object RecordingSession {
    private val _uiState = MutableStateFlow(RecordingUiState())
    val uiState: StateFlow<RecordingUiState> = _uiState.asStateFlow()

    fun update(block: (RecordingUiState) -> RecordingUiState) {
        _uiState.value = block(_uiState.value)
    }

    fun setMessage(value: String?) {
        _uiState.value = _uiState.value.copy(message = value)
    }
}
