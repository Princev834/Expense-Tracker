package com.princevekariya.projectledger.core.model

import java.util.Locale

data class Merchant(
    val id: String,
    val name: String,
    val isArchived: Boolean = false,
) {
    val searchKey: String
        get() = name
            .trim()
            .lowercase(Locale.ENGLISH)

    init {
        requireModelIdentifier(
            value = id,
            fieldName = "Merchant identifier",
        )
        require(name.isNotBlank()) {
            "Merchant name cannot be blank."
        }
    }
}
