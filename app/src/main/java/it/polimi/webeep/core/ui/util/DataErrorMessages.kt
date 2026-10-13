package it.polimi.webeep.core.ui.util

import androidx.annotation.StringRes
import it.polimi.webeep.R
import it.polimi.webeep.core.common.DataError

/** Localized message for a [DataError]; lives in the UI layer because it depends on Android resources. */
@StringRes
fun DataError.toMessageRes(): Int = when (this) {
    DataError.Network -> R.string.error_data_network
    DataError.Unauthorized -> R.string.error_session_expired
    is DataError.Server -> R.string.error_data_server
    DataError.Storage -> R.string.error_data_storage
    DataError.Unknown -> R.string.error_data_unknown
}
