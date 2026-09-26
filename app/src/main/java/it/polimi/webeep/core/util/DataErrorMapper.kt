package it.polimi.webeep.core.util

import androidx.annotation.StringRes
import it.polimi.webeep.R
import it.polimi.webeep.core.network.MoodleException
import java.io.IOException

private val UNAUTHORIZED_MOODLE_CODES = setOf("invalidtoken", "accessexception")

fun Throwable.toDataError(): DataError = when (this) {
    // MoodleException is an IOException: it must be matched first.
    is MoodleException ->
        if (errorCode in UNAUTHORIZED_MOODLE_CODES) DataError.Unauthorized else DataError.Server(errorCode)
    is IOException -> DataError.Network
    // Repositories throw IllegalStateException when no token is stored.
    is IllegalStateException ->
        if (message?.contains("token", ignoreCase = true) == true) DataError.Unauthorized else DataError.Unknown
    else -> DataError.Unknown
}

@StringRes
fun DataError.toMessageRes(): Int = when (this) {
    DataError.Network -> R.string.error_data_network
    DataError.Unauthorized -> R.string.error_session_expired
    is DataError.Server -> R.string.error_data_server
    DataError.Unknown -> R.string.error_data_unknown
}
