package it.polimi.webeep.feature.auth

import it.polimi.webeep.core.network.dto.SiteInfoDto
import it.polimi.webeep.core.util.Resource
import it.polimi.webeep.feature.auth.domain.AuthRepository
import it.polimi.webeep.feature.auth.domain.LoginUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Base64

class LoginUseCaseTest {

    private lateinit var loginUseCase: LoginUseCase
    private lateinit var fakeAuthRepository: FakeAuthRepository

    private val sampleSiteInfo = SiteInfoDto(
        sitename = "WeBeep",
        username = "mariorossi",
        firstname = "Mario",
        lastname = "Rossi",
        fullname = "Mario Rossi",
        lang = "it",
        userid = 12345,
        siteurl = "https://webeep.polimi.it",
        userpictureurl = "avatar.png",
        downloadfiles = 1,
        release = "4.2",
        version = "123",
        functions = emptyList()
    )

    @Before
    fun setup() {
        fakeAuthRepository = FakeAuthRepository()
        loginUseCase = LoginUseCase(fakeAuthRepository)
    }

    @Test
    fun `execute with valid encoded SSO token decodes, validates and saves auth data`() = runTest {
        val rawTokenData = "passport_hash:::wstoken_123:::private_token_456"
        val encodedToken = Base64.getEncoder().encodeToString(rawTokenData.toByteArray())
        
        fakeAuthRepository.validateResult = Result.success(sampleSiteInfo)

        val result = loginUseCase.execute(encodedToken)

        assertTrue(result is Resource.Success)
        assertEquals(sampleSiteInfo, (result as Resource.Success).data)
        
        assertEquals("wstoken_123", fakeAuthRepository.savedToken)
        assertEquals("private_token_456", fakeAuthRepository.savedPrivateToken)
        assertEquals(sampleSiteInfo, fakeAuthRepository.savedSiteInfo)
    }

    @Test
    fun `execute with invalid base64 string returns Resource Error`() = runTest {
        val invalidBase64 = "!!!NotBase64!!!"
        
        val result = loginUseCase.execute(invalidBase64)
        
        assertTrue(result is Resource.Error)
        assertTrue((result as Resource.Error).message.contains("Token di accesso non valido"))
    }

    @Test
    fun `execute preserves plus characters in base64 token`() = runTest {
        val encodedToken = generateEncodedTokenContainingPlus()

        fakeAuthRepository.validateResult = Result.success(sampleSiteInfo)

        val result = loginUseCase.execute(encodedToken)

        assertTrue(result is Resource.Success)
        assertEquals("wstoken_123", fakeAuthRepository.savedToken)
    }

    @Test
    fun `execute decodes percent encoded plus characters in base64 token`() = runTest {
        val encodedToken = generateEncodedTokenContainingPlus().replace("+", "%2B")

        fakeAuthRepository.validateResult = Result.success(sampleSiteInfo)

        val result = loginUseCase.execute(encodedToken)

        assertTrue(result is Resource.Success)
        assertEquals("wstoken_123", fakeAuthRepository.savedToken)
    }

    @Test
    fun `execute with base64 having missing segments returns Resource Error`() = runTest {
        val invalidRawData = "only_one_segment"
        val encodedToken = Base64.getEncoder().encodeToString(invalidRawData.toByteArray())
        
        val result = loginUseCase.execute(encodedToken)
        
        assertTrue(result is Resource.Error)
        assertEquals("Formato token non valido", (result as Resource.Error).message)
    }

    @Test
    fun `execute with server validation failure returns Resource Error and saves nothing`() = runTest {
        val rawTokenData = "passport_hash:::wstoken_123:::private_token_456"
        val encodedToken = Base64.getEncoder().encodeToString(rawTokenData.toByteArray())
        
        fakeAuthRepository.validateResult = Result.failure(Exception("Token non valido"))

        val result = loginUseCase.execute(encodedToken)

        assertTrue(result is Resource.Error)
        assertEquals("Token non valido", (result as Resource.Error).message)
        assertTrue(fakeAuthRepository.savedToken == null)
    }

    private class FakeAuthRepository : AuthRepository {
        var validateResult: Result<SiteInfoDto> = Result.failure(Exception("Not initialized"))
        var savedToken: String? = null
        var savedPrivateToken: String? = null
        var savedSiteInfo: SiteInfoDto? = null
        var isLoggedInValue: Boolean = false

        override suspend fun validateToken(token: String): Result<SiteInfoDto> {
            return validateResult
        }

        override suspend fun saveAuthData(token: String, privateToken: String?, siteInfo: SiteInfoDto) {
            savedToken = token
            savedPrivateToken = privateToken
            savedSiteInfo = siteInfo
            isLoggedInValue = true
        }

        override suspend fun isLoggedIn(): Boolean = isLoggedInValue

        override suspend fun logout() {
            isLoggedInValue = false
        }
    }

    private fun generateEncodedTokenContainingPlus(): String {
        val rawTokenData = "passport_hash:::wstoken_123:::private_token_456:::>"
        return Base64.getEncoder().encodeToString(rawTokenData.toByteArray()).also { encodedToken ->
            check("+" in encodedToken)
        }
    }
}
