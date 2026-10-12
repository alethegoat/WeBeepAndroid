package it.polimi.webeep.core.common

/**
 * Typed data-layer failure. The presentation layer maps it to a localized
 * string, so no technical exception text ever reaches the UI.
 */
sealed interface DataError {
    /** No connectivity or a transport-level failure; usually retryable. */
    data object Network : DataError

    /** The token is missing, expired or rejected: the session must be re-established by logging in again. */
    data object Unauthorized : DataError

    /** Moodle answered with an application error; [errorCode] is Moodle's machine-readable code. */
    data class Server(val errorCode: String) : DataError

    /** Local disk failure: full storage, missing permissions or an unusable path. */
    data object Storage : DataError

    /** Anything not classified above. */
    data object Unknown : DataError
}
