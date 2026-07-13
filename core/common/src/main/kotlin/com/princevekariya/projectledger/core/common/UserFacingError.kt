package com.princevekariya.projectledger.core.common

import java.io.IOException

sealed interface UserFacingError {
    val title: String
    val message: String
    val canRetry: Boolean

    data object PermissionRequired : UserFacingError {
        override val title: String = "Permission required"
        override val message: String = "Allow the required permission and try again."
        override val canRetry: Boolean = true
    }

    data object ConnectionUnavailable : UserFacingError {
        override val title: String = "Connection unavailable"
        override val message: String = "Check your internet connection and try again."
        override val canRetry: Boolean = true
    }

    data object InvalidInput : UserFacingError {
        override val title: String = "Check your entry"
        override val message: String = "Review the entered details and try again."
        override val canRetry: Boolean = false
    }

    data object Unexpected : UserFacingError {
        override val title: String = "Something went wrong"
        override val message: String = "The action could not be completed. Please try again."
        override val canRetry: Boolean = true
    }
}

fun Throwable.toUserFacingError(): UserFacingError = when (this) {
    is SecurityException -> UserFacingError.PermissionRequired
    is IOException -> UserFacingError.ConnectionUnavailable
    is IllegalArgumentException -> UserFacingError.InvalidInput
    else -> UserFacingError.Unexpected
}
