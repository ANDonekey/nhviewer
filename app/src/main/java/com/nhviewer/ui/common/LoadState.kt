package com.nhviewer.ui.common

sealed interface LoadState<out T> {
    data object Loading : LoadState<Nothing>
    data object Empty : LoadState<Nothing>
    data class Content<T>(val value: T) : LoadState<T>
    data class Error(val message: String) : LoadState<Nothing>
}
