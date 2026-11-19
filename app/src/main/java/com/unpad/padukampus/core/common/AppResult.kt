package com.unpad.padukampus.core.common

/** Errors can carry cached data; a refresh failure never erases local content. */
sealed interface AppResult<out T> {
    data class Loading<T>(val data: T? = null) : AppResult<T>

    data class Success<T>(val data: T) : AppResult<T>

    data class Error<T>(val message: String, val data: T? = null) : AppResult<T>
}

fun <T> AppResult<T>.dataOrNull(): T? =
    when (this) {
        is AppResult.Loading -> data
        is AppResult.Success -> data
        is AppResult.Error -> data
    }
