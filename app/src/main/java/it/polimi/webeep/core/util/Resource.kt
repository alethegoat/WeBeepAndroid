package it.polimi.webeep.core.util

/**
 * sealed class representing the state of an asynchronous operation.
 * used as a wrapper for api call results and database operations.
 *
 * @param T the type of data contained in the result upon success.
 */
sealed class Resource<out T> {

    /**
     * success state with the resulting data.
     *
     * @param data the data returned by the operation.
     */
    data class Success<T>(val data: T) : Resource<T>()

    /**
     * error state with descriptive message and optional throwable.
     *
     * @param message descriptive error message.
     * @param throwable the original exception, if available.
     */
    data class Error(val message: String, val throwable: Throwable? = null) : Resource<Nothing>()

    /**
     * loading state — the operation is in progress.
     */
    data object Loading : Resource<Nothing>()
}
