package it.polimi.webeep.feature.auth.domain

import it.polimi.webeep.core.common.DataError

/**
 * Stable login failure reasons. The UI maps these to localized strings,
 * so domain messages never leak untranslated text to the user.
 */
sealed interface LoginError {
    /** The callback payload is not in the expected `hash:::token` shape. */
    data object InvalidTokenFormat : LoginError

    /** No pending SSO passport is stored: the login attempt timed out or was never started here. */
    data object SessionExpired : LoginError

    /** The passport hash does not match any pending login, so the callback is not ours. */
    data object InvalidPassport : LoginError

    /** The payload is not valid Base64. */
    data object InvalidAccessToken : LoginError

    /** Decoding failed for another reason. */
    data object TokenProcessingFailed : LoginError

    /** The token decoded fine but Moodle rejected it or it could not be validated or stored; [error] says why. */
    data class ValidationFailed(val error: DataError) : LoginError
}

/** Outcome of [LoginUseCase.execute]. */
sealed interface LoginResult {
    /** Login completed and the session is stored. */
    data class Success(val session: UserSession) : LoginResult

    /** Login failed; nothing was stored. */
    data class Failure(val error: LoginError) : LoginResult
}
