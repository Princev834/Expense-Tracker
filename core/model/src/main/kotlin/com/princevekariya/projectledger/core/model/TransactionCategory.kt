package com.princevekariya.projectledger.core.model

data class TransactionCategory(
    val id: String,
    val name: String,
    val type: CategoryType,
    val iconKey: String,
    val isDefault: Boolean = false,
    val isArchived: Boolean = false,
) {
    init {
        requireModelIdentifier(
            value = id,
            fieldName = "Category identifier",
        )
        require(name.isNotBlank()) {
            "Category name cannot be blank."
        }
        require(iconKey.isNotBlank()) {
            "Category icon key cannot be blank."
        }
    }
}
