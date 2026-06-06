package it.polimi.webeep.feature.auth.domain

import it.polimi.webeep.core.network.dto.SiteInfoDto
import it.polimi.webeep.core.util.Resource
import java.net.URLDecoder
import java.util.Base64
import javax.inject.Inject

/**
 * use case to handle login via webeep sso.
 * decodes the base64 token and validates it via api.
 */
class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /**
     * executes the login flow starting from the base64 encoded token.
     *
     * @param encodedToken the base64 encoded token (e.g. passport:::wstoken:::privateToken).
     * @return [Resource] with the login result.
     */
    suspend fun execute(encodedToken: String): Resource<SiteInfoDto> {
        val (wstoken, privateToken) = try {
            val decodedBytes = Base64.getDecoder().decode(decodeTokenParameter(encodedToken))
            val decodedString = String(decodedBytes, Charsets.UTF_8)
            
            // the moodle format is passport_hash:::wstoken:::private_token
            val parts = decodedString.split(":::")
            if (parts.size < 2) {
                return Resource.Error("invalid token format")
            }
            
            val wsToken = parts[1]
            val privateToken = parts.getOrNull(2)
            
            wsToken to privateToken
        } catch (e: IllegalArgumentException) {
            return Resource.Error("invalid access token. please try logging in with sso again.", e)
        } catch (e: Exception) {
            return Resource.Error("unable to process access token. please try again.", e)
        }

        val result = authRepository.validateToken(wstoken)
        return if (result.isSuccess) {
            val siteInfo = result.getOrThrow()
            // save the extracted token securely for subsequent api calls
            authRepository.saveAuthData(wstoken, privateToken, siteInfo)
            Resource.Success(siteInfo)
        } else {
            val exception = result.exceptionOrNull()
            Resource.Error(
                message = exception?.localizedMessage ?: "unable to validate token with moodle server",
                throwable = exception
            )
        }
    }

    private fun decodeTokenParameter(encodedToken: String): String {
        val token = encodedToken.trim()
        return try {
            URLDecoder.decode(token.replace("+", "%2B"), Charsets.UTF_8.name())
        } catch (e: Exception) {
            token
        }
    }
}
