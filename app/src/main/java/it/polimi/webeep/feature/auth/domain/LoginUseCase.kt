package it.polimi.webeep.feature.auth.domain

import it.polimi.webeep.core.datastore.TokenProvider
import java.net.URLDecoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.inject.Inject

/**
 * Handles login via WeBeep SSO.
 * Decodes the base64 token, verifies the Moodle passport hash, then validates via API.
 * Failures are typed ([LoginError]) so the UI can localize them.
 */
class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val tokenProvider: TokenProvider
) {
    fun generateSsoLaunchUrl(): String {
        val passport = generatePassport()
        tokenProvider.savePendingPassportSync(passport)
        return buildSsoLaunchUrl(passport)
    }

    suspend fun execute(encodedToken: String): LoginResult {
        val wstoken = try {
            val decodedBytes = decodeBase64Token(decodeTokenParameter(encodedToken))
            val decodedString = String(decodedBytes, Charsets.UTF_8)

            val parts = decodedString.split(":::")
            if (parts.size < 2) {
                return LoginResult.Failure(LoginError.InvalidTokenFormat)
            }

            val passports = tokenProvider.getPendingPassportsSync()
            if (passports.isEmpty()) {
                return LoginResult.Failure(LoginError.SessionExpired)
            }
            val receivedHash = parts[0].trim()
            val passportMatches = passports.any { passport ->
                passportHashes(passport).any { it.equals(receivedHash, ignoreCase = true) }
            }
            if (!passportMatches) {
                return LoginResult.Failure(LoginError.InvalidPassport)
            }

            // parts[2] is the autologin-capable private token: intentionally discarded.
            parts[1].trim().ifEmpty { return LoginResult.Failure(LoginError.InvalidTokenFormat) }
        } catch (e: IllegalArgumentException) {
            return LoginResult.Failure(LoginError.InvalidAccessToken)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            return LoginResult.Failure(LoginError.TokenProcessingFailed)
        }

        return try {
            val result = authRepository.validateToken(wstoken)
            if (result.isSuccess) {
                val siteInfo = result.getOrThrow()
                authRepository.saveAuthData(wstoken, null, siteInfo)
                tokenProvider.clearPendingPassportSync()
                LoginResult.Success(siteInfo)
            } else {
                LoginResult.Failure(LoginError.ValidationFailed(result.exceptionOrNull()))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            LoginResult.Failure(LoginError.ValidationFailed(e))
        }
    }

    private fun decodeTokenParameter(encodedToken: String): String {
        val token = encodedToken.trim()
        return try {
            URLDecoder.decode(token.replace("+", "%2B"), Charsets.UTF_8.name())
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            token
        }
    }

    companion object {
        // Single source for host: keep in sync with NetworkModule.BASE_URL.
        const val SITE_WWWROOT = "https://" + it.polimi.webeep.core.network.MoodleFileUrlBuilder.TRUSTED_HOST
        const val SSO_SERVICE = "moodle_mobile_app"
        const val SSO_URL_SCHEME = "webeepandroid"

        fun generatePassport(): String {
            // Official Moodle app sends Math.random() * 1000 (numeric). Older Moodle
            // used PARAM_INT, which would collapse a UUID to 0 and break the hash.
            return (SecureRandom().nextInt(Int.MAX_VALUE - 1) + 1).toString()
        }

        fun buildSsoLaunchUrl(passport: String): String =
            "$SITE_WWWROOT/admin/tool/mobile/launch.php" +
                "?service=$SSO_SERVICE" +
                "&passport=$passport" +
                "&urlscheme=$SSO_URL_SCHEME"

        /**
         * Moodle 4+/5 launch.php hashes md5($CFG->wwwroot . $passport).
         * Official app also retries http/https. Keep bare md5(passport) for older sites.
         */
        fun passportHashes(passport: String): Set<String> {
            val hashes = LinkedHashSet<String>(5)
            hashes.add(md5Hex(passport))
            for (root in wwwrootVariants()) {
                hashes.add(md5Hex(root + passport))
            }
            return hashes
        }

        fun md5Hex(input: String): String {
            val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray(Charsets.UTF_8))
            return bytes.joinToString("") { "%02x".format(it) }
        }

        fun decodeBase64Token(encoded: String): ByteArray {
            val cleaned = encoded.replace("\\s".toRegex(), "")
            return try {
                Base64.getDecoder().decode(cleaned)
            } catch (_: IllegalArgumentException) {
                Base64.getUrlDecoder().decode(cleaned)
            }
        }

        private fun wwwrootVariants(): List<String> {
            val https = SITE_WWWROOT
            val http = SITE_WWWROOT.replaceFirst("https://", "http://")
            return listOf(https, "$https/", http, "$http/")
        }
    }
}
