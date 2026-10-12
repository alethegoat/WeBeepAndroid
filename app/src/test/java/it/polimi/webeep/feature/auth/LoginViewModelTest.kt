package it.polimi.webeep.feature.auth

import app.cash.turbine.test
import it.polimi.webeep.R
import it.polimi.webeep.core.common.DataError
import it.polimi.webeep.core.common.Resource
import it.polimi.webeep.feature.auth.domain.LoginUseCase
import it.polimi.webeep.feature.auth.domain.SsoCallbackRelay
import it.polimi.webeep.feature.auth.domain.UserSession
import it.polimi.webeep.feature.auth.presentation.LoginEffect
import it.polimi.webeep.feature.auth.presentation.LoginUiState
import it.polimi.webeep.feature.auth.presentation.LoginViewModel
import it.polimi.webeep.util.MainDispatcherRule
import it.polimi.webeep.util.fakes.FakeAuthRepository
import it.polimi.webeep.util.fakes.FakeSyncScheduling
import it.polimi.webeep.util.fakes.FakeTokenProvider
import java.util.Base64
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val passport = "test-passport-value"
    private val session = UserSession(1, "Mario Rossi", "mariorossi", "https://webeep.polimi.it", "avatar.png")

    private val auth = FakeAuthRepository()
    private val tokens = FakeTokenProvider().also { it.savePendingPassportSync(passport) }
    private val scheduling = FakeSyncScheduling()
    private val relay = SsoCallbackRelay()

    private fun viewModel() = LoginViewModel(
        loginUseCase = LoginUseCase(auth, tokens),
        authRepository = auth,
        syncScheduler = scheduling,
        ssoCallbackRelay = relay
    )

    private fun validToken(): String {
        val raw = "${LoginUseCase.md5Hex(passport)}:::wstoken_123:::private"
        return Base64.getEncoder().encodeToString(raw.toByteArray())
    }

    @Test
    fun `starts idle`() = runTest {
        assertEquals(LoginUiState.Idle, viewModel().uiState.value)
    }

    @Test
    fun `an existing session emits LoggedIn`() = runTest {
        auth.isLoggedInValue = true

        viewModel().effects.test {
            assertEquals(LoginEffect.LoggedIn, awaitItem())
        }
    }

    @Test
    fun `a valid SSO callback shows loading then emits LoggedIn and schedules sync`() = runTest {
        auth.validateResult = Resource.Success(session)
        val vm = viewModel()

        vm.effects.test {
            relay.offer(validToken())

            assertEquals(LoginEffect.LoggedIn, awaitItem())
            assertEquals(LoginUiState.Idle, vm.uiState.value)
            assertEquals("wstoken_123", auth.savedToken)
            assertEquals(1, scheduling.applyCount)
            assertEquals(listOf(false), scheduling.immediateRequests)
        }
    }

    @Test
    fun `the callback shows loading while it is being validated`() = runTest {
        auth.validateResult = Resource.Success(session)
        val vm = viewModel()

        vm.uiState.test {
            assertEquals(LoginUiState.Idle, awaitItem())
            relay.offer(validToken())
            assertEquals(LoginUiState.Loading, awaitItem())
            assertEquals(LoginUiState.Idle, awaitItem())
        }
    }

    @Test
    fun `blank and placeholder callbacks are ignored`() = runTest {
        val vm = viewModel()

        vm.effects.test {
            relay.offer("   ")
            relay.offer("{token}")
            expectNoEvents()
        }
        assertEquals(LoginUiState.Idle, vm.uiState.value)
    }

    @Test
    fun `a malformed token emits ShowError with the invalid token text`() = runTest {
        val vm = viewModel()

        vm.effects.test {
            relay.offer("!!!NotBase64!!!")

            assertEquals(LoginEffect.ShowError(R.string.error_login_invalid_token), awaitItem())
            assertEquals(LoginUiState.Idle, vm.uiState.value)
        }
    }

    @Test
    fun `a token without separators emits the invalid format error`() = runTest {
        viewModel().effects.test {
            relay.offer(Base64.getEncoder().encodeToString("nope".toByteArray()))

            assertEquals(LoginEffect.ShowError(R.string.error_login_invalid_format), awaitItem())
        }
    }

    @Test
    fun `an expired passport emits the session expired error`() = runTest {
        tokens.clearPendingPassportSync()

        viewModel().effects.test {
            relay.offer(validToken())

            assertEquals(LoginEffect.ShowError(R.string.error_login_session_expired), awaitItem())
        }
    }

    @Test
    fun `a wrong passport hash emits the invalid passport error`() = runTest {
        val raw = "${LoginUseCase.md5Hex("other")}:::wstoken_123:::private"

        viewModel().effects.test {
            relay.offer(Base64.getEncoder().encodeToString(raw.toByteArray()))

            assertEquals(LoginEffect.ShowError(R.string.error_login_invalid_passport), awaitItem())
        }
    }

    @Test
    fun `a rejected token emits the validation error`() = runTest {
        auth.validateResult = Resource.Error(DataError.Unauthorized)

        viewModel().effects.test {
            relay.offer(validToken())

            val effect = awaitItem()
            assertTrue(effect is LoginEffect.ShowError)
            assertEquals(R.string.error_login_validation_failed, (effect as LoginEffect.ShowError).messageRes)
        }
    }
}
