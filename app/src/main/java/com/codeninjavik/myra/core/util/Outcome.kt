package com.codeninjavik.myra.core.util

sealed class Outcome<out T> {
    object Loading : Outcome<Nothing>()
    data class Success<out T>(val data: T) : Outcome<T>()
    data class Failure(val errorMsg: String, val throwable: Throwable? = null) : Outcome<Nothing>()
}
