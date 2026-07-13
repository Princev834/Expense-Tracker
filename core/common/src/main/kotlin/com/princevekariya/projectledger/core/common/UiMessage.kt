package com.princevekariya.projectledger.core.common

data class UiMessage(
    val id: Long,
    val text: String,
) {
    init {
        require(text.isNotBlank()) {
            "A UI message must contain user-readable text."
        }
    }
}
