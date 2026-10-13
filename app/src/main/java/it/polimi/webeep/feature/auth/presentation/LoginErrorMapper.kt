package it.polimi.webeep.feature.auth.presentation

import androidx.annotation.StringRes
import it.polimi.webeep.R
import it.polimi.webeep.feature.auth.domain.LoginError

@StringRes
fun LoginError.toMessageRes(): Int = when (this) {
    LoginError.InvalidTokenFormat -> R.string.error_login_invalid_format
    LoginError.SessionExpired -> R.string.error_login_session_expired
    LoginError.InvalidPassport -> R.string.error_login_invalid_passport
    LoginError.InvalidAccessToken -> R.string.error_login_invalid_token
    LoginError.TokenProcessingFailed -> R.string.error_login_process_failed
    // Every validation failure keeps the same text; the cause is only for logs and diagnostics.
    is LoginError.ValidationFailed -> R.string.error_login_validation_failed
}
