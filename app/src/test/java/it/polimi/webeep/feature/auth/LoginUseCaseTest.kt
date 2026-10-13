package it.polimi.webeep.feature.auth

import it.polimi.webeep.core.common.DataError
import it.polimi.webeep.core.common.Resource
import it.polimi.webeep.core.network.MoodleFileUrlBuilder
import it.polimi.webeep.feature.auth.domain.LoginError
import it.polimi.webeep.feature.auth.domain.LoginResult
import it.polimi.webeep.feature.auth.domain.LoginUseCase
import it.polimi.webeep.feature.auth.domain.UserSession
import it.polimi.webeep.util.fakes.FakeAuthRepository
import it.polimi.webeep.util.fakes.FakeTokenProvider
import java.util.Base64
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LoginUseCaseTest {

    private lateinit var loginUseCase: LoginUseCase
    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var fakeTokenProvider: FakeTokenProvider

    private val passport = "test-passport-value"

    private val sampleSession = UserSession(
        userId = 12345,
        fullName = "Mario Rossi",
        userName = "mariorossi",
        siteUrl = "https://webeep.polimi.it",
        avatarUrl = "avatar.png"
    )

    @Before
    fun setup() {
        fakeAuthRepository = FakeAuthRepository()
        fakeTokenProvider = FakeTokenProvider()
        fakeTokenProvider.savePendingPassportSync(passport)
        loginUseCase = LoginUseCase(fakeAuthRepository, fakeTokenProvider)
    }

    @Test
    fun `execute with valid encoded SSO token decodes, validates and saves auth data`() = runTest {
        val encodedToken = encodeSsoPayload(LoginUseCase.md5Hex(passport), "wstoken_123", "private_token_456")
        fakeAuthRepository.validateResult = Resource.Success(sampleSession)

        val result = loginUseCase.execute(encodedToken)

        assertTrue(result is LoginResult.Success)
        assertEquals(sampleSession, (result as LoginResult.Success).session)
        assertEquals("wstoken_123", fakeAuthRepository.savedToken)
        assertEquals(sampleSession, fakeAuthRepository.savedSession)
        assertNull(fakeTokenProvider.getPendingPassportsSync().lastOrNull())
    }

    @Test
    fun `execute with expired or missing passports returns SessionExpired and saves nothing`() = runTest {
        val encodedToken = encodeSsoPayload(LoginUseCase.md5Hex(passport), "wstoken_123", "private_token_456")
        fakeAuthRepository.validateResult = Resource.Success(sampleSession)
        // TokenManager drops passports older than the TTL, so an expired one reads as none.
        fakeTokenProvider.clearPendingPassportSync()

        val result = loginUseCase.execute(encodedToken)

        assertTrue(result is LoginResult.Failure)
        assertEquals(LoginError.SessionExpired, (result as LoginResult.Failure).error)
        assertNull(fakeAuthRepository.savedToken)
    }

    @Test
    fun `execute with invalid base64 string returns InvalidAccessToken failure`() = runTest {
        val invalidBase64 = "!!!NotBase64!!!"

        val result = loginUseCase.execute(invalidBase64)

        assertTrue(result is LoginResult.Failure)
        assertEquals(LoginError.InvalidAccessToken, (result as LoginResult.Failure).error)
    }

    @Test
    fun `execute preserves plus characters in base64 token`() = runTest {
        val encodedToken = generateEncodedTokenContainingPlus()
        fakeAuthRepository.validateResult = Resource.Success(sampleSession)

        val result = loginUseCase.execute(encodedToken)

        assertTrue(result is LoginResult.Success)
        assertEquals("wstoken_123", fakeAuthRepository.savedToken)
    }

    @Test
    fun `execute decodes percent encoded plus characters in base64 token`() = runTest {
        val encodedToken = generateEncodedTokenContainingPlus().replace("+", "%2B")
        fakeAuthRepository.validateResult = Resource.Success(sampleSession)

        val result = loginUseCase.execute(encodedToken)

        assertTrue(result is LoginResult.Success)
        assertEquals("wstoken_123", fakeAuthRepository.savedToken)
    }

    @Test
    fun `execute with base64 having missing segments returns InvalidTokenFormat failure`() = runTest {
        val encodedToken = Base64.getEncoder().encodeToString("only_one_segment".toByteArray())

        val result = loginUseCase.execute(encodedToken)

        assertTrue(result is LoginResult.Failure)
        assertEquals(LoginError.InvalidTokenFormat, (result as LoginResult.Failure).error)
    }

    @Test
    fun `execute accepts Moodle siteid hash of wwwroot plus passport`() = runTest {
        val encodedToken = encodeSsoPayload(
            LoginUseCase.md5Hex(LoginUseCase.SITE_WWWROOT + passport),
            "wstoken_123",
            "private_token_456"
        )
        fakeAuthRepository.validateResult = Resource.Success(sampleSession)

        val result = loginUseCase.execute(encodedToken)

        assertTrue(result is LoginResult.Success)
        assertEquals("wstoken_123", fakeAuthRepository.savedToken)
        assertNull(fakeTokenProvider.getPendingPassportsSync().lastOrNull())
    }

    @Test
    fun `execute accepts http wwwroot variant used by official Moodle app`() = runTest {
        val httpRoot = LoginUseCase.SITE_WWWROOT.replaceFirst("https://", "http://")
        val encodedToken = encodeSsoPayload(
            LoginUseCase.md5Hex(httpRoot + passport),
            "wstoken_123",
            "private_token_456"
        )
        fakeAuthRepository.validateResult = Resource.Success(sampleSession)

        val result = loginUseCase.execute(encodedToken)

        assertTrue(result is LoginResult.Success)
        assertEquals("wstoken_123", fakeAuthRepository.savedToken)
    }

    @Test
    fun `execute accepts an earlier passport after a second login tap`() = runTest {
        val firstPassport = "first-passport"
        fakeTokenProvider.savePendingPassportSync(firstPassport)
        fakeTokenProvider.savePendingPassportSync(passport)
        fakeAuthRepository.validateResult = Resource.Success(sampleSession)

        val encodedToken = encodeSsoPayload(LoginUseCase.md5Hex(firstPassport), "wstoken_123", "private_token_456")
        val result = loginUseCase.execute(encodedToken)

        assertTrue(result is LoginResult.Success)
        assertEquals("wstoken_123", fakeAuthRepository.savedToken)
    }

    @Test
    fun `generateSsoLaunchUrl stores a numeric passport like the official Moodle app`() {
        fakeTokenProvider.clearPendingPassportSync()

        val url = loginUseCase.generateSsoLaunchUrl()
        val stored = fakeTokenProvider.getPendingPassportsSync().lastOrNull()

        assertTrue(stored != null && stored.all { it.isDigit() })
        assertTrue(url.startsWith("${LoginUseCase.SITE_WWWROOT}/admin/tool/mobile/launch.php"))
        assertTrue(url.contains("passport=$stored"))
        assertTrue(url.contains("urlscheme=${LoginUseCase.SSO_URL_SCHEME}"))
        assertTrue(url.contains("service=${LoginUseCase.SSO_SERVICE}"))
    }

    @Test
    fun `execute with mismatched passport returns InvalidPassport failure and saves nothing`() = runTest {
        val encodedToken = encodeSsoPayload("wronghash", "wstoken_123", "private_token_456")
        fakeAuthRepository.validateResult = Resource.Success(sampleSession)

        val result = loginUseCase.execute(encodedToken)

        assertTrue(result is LoginResult.Failure)
        assertEquals(LoginError.InvalidPassport, (result as LoginResult.Failure).error)
        assertNull(fakeAuthRepository.savedToken)
    }

    @Test
    fun `execute with server validation failure returns ValidationFailed and saves nothing`() = runTest {
        val encodedToken = encodeSsoPayload(LoginUseCase.md5Hex(passport), "wstoken_123", "private_token_456")
        fakeAuthRepository.validateResult = Resource.Error(DataError.Unauthorized)

        val result = loginUseCase.execute(encodedToken)

        assertTrue(result is LoginResult.Failure)
        val error = (result as LoginResult.Failure).error
        assertTrue(error is LoginError.ValidationFailed)
        assertEquals(DataError.Unauthorized, (error as LoginError.ValidationFailed).error)
        assertTrue(fakeAuthRepository.savedToken == null)
    }

    @Test
    fun `site root stays pinned to the trusted host`() {
        assertEquals("https://" + MoodleFileUrlBuilder.TRUSTED_HOST, LoginUseCase.SITE_WWWROOT)
    }

    private fun encodeSsoPayload(hash: String, wsToken: String, privateToken: String): String {
        val raw = "$hash:::$wsToken:::$privateToken"
        return Base64.getEncoder().encodeToString(raw.toByteArray())
    }

    private fun generateEncodedTokenContainingPlus(): String {
        val hash = LoginUseCase.md5Hex(passport)
        for (extra in 0 until 10_000) {
            val rawTokenData = "$hash:::wstoken_123:::private_token_456:::pad-$extra->"
            val encodedToken = Base64.getEncoder().encodeToString(rawTokenData.toByteArray())
            if ('+' in encodedToken) return encodedToken
        }
        error("could not generate a base64 SSO token containing '+'")
    }
}
