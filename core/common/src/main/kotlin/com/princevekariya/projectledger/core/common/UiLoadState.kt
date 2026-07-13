package com.princevekariya.projectledger.core.common

sealed interface UiLoadState {
    data object Idle : UiLoadState

    data object Loading : UiLoadState

    data object Content : UiLoadState

    data class Error(
        val message: String,
    ) : UiLoadState {
        init {
            require(message.isNotBlank()) {
                "An error state must contain a user-readable message."
            }
        }
    }
}
