package it.polimi.webeep.feature.auth.domain

/**
 * Stable login failure reasons. The UI maps these to localized strings,
 * so domain messages never leak untranslated text to the user.
 */
sealed interface LoginError {
    data object InvalidTokenFormat : LoginError
    data object SessionExpired : LoginError
    data object InvalidPassport : LoginError
    data object InvalidAccessToken : LoginError
    data object TokenProcessingFailed : LoginError
    data class ValidationFailed(val cause: Throwable? = null) : LoginError
}

sealed interface LoginResult {
    data class Success(val session: UserSession) : LoginResult
    data class Failure(val error: LoginError) : LoginResult
}
