package com.princevekariya.projectledger.core.model

internal fun requireModelIdentifier(value: String, fieldName: String) {
    require(value.isNotBlank()) {
        "$fieldName cannot be blank."
    }
}

internal fun requireOptionalModelIdentifier(value: String?, fieldName: String) {
    if (value != null) {
        requireModelIdentifier(
            value = value,
            fieldName = fieldName,
        )
    }
}
