package it.polimi.webeep.core.util

sealed class Resource<out T> {

    data class Success<T>(val data: T) : Resource<T>()

    data class Error(val error: DataError, val throwable: Throwable? = null) : Resource<Nothing>()

    data object Loading : Resource<Nothing>()
}
