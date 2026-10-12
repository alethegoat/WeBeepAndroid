package it.polimi.webeep.feature.auth.domain

import it.polimi.webeep.core.common.Resource
import it.polimi.webeep.core.common.resourceOf
import it.polimi.webeep.core.common.runCatchingCancellable
import it.polimi.webeep.core.datastore.TokenProvider
import java.net.URLDecoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

/**
 * Handles login via WeBeep SSO.
 * Decodes the base64 token, verifies the Moodle passport hash, then validates via API.
 * Failures are typed ([LoginError]) so the UI can localize them.
 */
class LoginUseCase @Inject constructor(private val authRepository: AuthRepository, private val tokenProvider: TokenProvider) {
    /** Starts a login attempt: stores a fresh passport as pending and returns the Moodle SSO URL that carries it. */
    fun generateSsoLaunchUrl(): String {
        val passport = generatePassport()
        tokenProvider.savePendingPassportSync(passport)
        return buildSsoLaunchUrl(passport)
    }

    // Linear validation chain: each check maps to a distinct LoginError.
    /**
     * Completes login from the callback blob [encodedToken]: checks the passport hash against pending logins,
     * validates the token with Moodle and stores the session. Only the access token is kept.
     */
    @Suppress("ReturnCount")
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

            // parts[2] is the private token. It grants a full web session, so it is deliberately dropped and never stored.
            parts[1].trim().ifEmpty { return LoginResult.Failure(LoginError.InvalidTokenFormat) }
        } catch (_: IllegalArgumentException) {
            return LoginResult.Failure(LoginError.InvalidAccessToken)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            return LoginResult.Failure(LoginError.TokenProcessingFailed)
        }

        val session = when (val validated = authRepository.validateToken(wstoken)) {
            is Resource.Error -> return LoginResult.Failure(LoginError.ValidationFailed(validated.error))
            is Resource.Success -> validated.data
        }
        return when (val saved = resourceOf { persistSession(wstoken, session) }) {
            is Resource.Success -> LoginResult.Success(session)
            is Resource.Error -> LoginResult.Failure(LoginError.ValidationFailed(saved.error))
        }
    }

    private suspend fun persistSession(wstoken: String, session: UserSession) {
        authRepository.saveAuthData(wstoken, session)
        tokenProvider.clearPendingPassportSync()
    }

    private fun decodeTokenParameter(encodedToken: String): String {
        val token = encodedToken.trim()
        return runCatchingCancellable { URLDecoder.decode(token.replace("+", "%2B"), Charsets.UTF_8.name()) }.getOrDefault(token)
    }

    /** Moodle SSO protocol constants and the stateless helpers of the passport handshake. */
    companion object {
        /**
         * Moodle site root, hashed into the passport check.
         * Must equal `MoodleFileUrlBuilder.TRUSTED_HOST` (a unit test pins it); the domain layer stays free of network code.
         */
        const val SITE_WWWROOT = "https://webeep.polimi.it"

        /** Moodle web-service short name requested at SSO launch. */
        const val SSO_SERVICE = "moodle_mobile_app"

        /** Custom URL scheme Moodle redirects to; must match the manifest intent filter. */
        const val SSO_URL_SCHEME = "webeepandroid"

        /**
         * Random positive integer as a string. It must stay numeric: older Moodle used `PARAM_INT`, which would
         * collapse a UUID to 0 and break the hash.
         */
        fun generatePassport(): String = (SecureRandom().nextInt(Int.MAX_VALUE - 1) + 1).toString()

        /** Moodle launch URL that carries [passport] and the app's URL scheme. */
        fun buildSsoLaunchUrl(passport: String): String = "$SITE_WWWROOT/admin/tool/mobile/launch.php" +
            "?service=$SSO_SERVICE" +
            "&passport=$passport" +
            "&urlscheme=$SSO_URL_SCHEME"

        /**
         * Moodle 4+/5 launch.php hashes md5($CFG->wwwroot . $passport).
         * Official app also retries http/https. Keep bare md5(passport) for older sites.
         */
        fun passportHashes(passport: String): Set<String> {
            val hashes = LinkedHashSet<String>()
            hashes.add(md5Hex(passport))
            for (root in wwwrootVariants()) {
                hashes.add(md5Hex(root + passport))
            }
            return hashes
        }

        /** Lowercase hex MD5; Moodle's launch protocol mandates it, it is not used for security here. */
        fun md5Hex(input: String): String {
            val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray(Charsets.UTF_8))
            return bytes.joinToString("") { "%02x".format(it) }
        }

        /** Decodes standard or URL-safe Base64, ignoring whitespace; throws `IllegalArgumentException` if neither fits. */
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
