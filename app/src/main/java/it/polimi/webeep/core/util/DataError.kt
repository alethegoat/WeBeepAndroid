package it.polimi.webeep.core.util

/**
 * Typed data-layer failure. The presentation layer maps it to a localized
 * string, so no technical exception text ever reaches the UI.
 */
sealed interface DataError {
    data object Network : DataError
    data object Unauthorized : DataError
    data class Server(val errorCode: String) : DataError

    /** Local disk failure: full storage, missing permissions or an unusable path. */
    data object Storage : DataError
    data object Unknown : DataError
}
