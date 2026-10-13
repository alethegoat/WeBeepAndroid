package it.polimi.webeep.core.common

import java.io.IOException
import kotlin.io.FileSystemException

private val UNAUTHORIZED_MOODLE_CODES = setOf("invalidtoken", "accessexception")

fun Throwable.toDataError(): DataError = when (this) {
    // MoodleException is an IOException: it must be matched first.
    is MoodleException ->
        if (errorCode in UNAUTHORIZED_MOODLE_CODES) DataError.Unauthorized else DataError.Server(errorCode)
    is FileSystemException -> DataError.Storage
    is IOException -> if (isOutOfSpace()) DataError.Storage else DataError.Network
    is MissingTokenException -> DataError.Unauthorized
    else -> DataError.Unknown
}

// Android surfaces a full disk as a plain IOException (ENOSPC), with no dedicated subclass.
private fun IOException.isOutOfSpace() = message?.contains("ENOSPC") == true || message?.contains("No space left") == true
