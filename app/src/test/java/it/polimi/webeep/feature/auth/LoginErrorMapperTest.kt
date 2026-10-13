package it.polimi.webeep.feature.auth

import it.polimi.webeep.R
import it.polimi.webeep.core.util.DataError
import it.polimi.webeep.feature.auth.domain.LoginError
import it.polimi.webeep.feature.auth.presentation.toMessageRes
import org.junit.Assert.assertEquals
import org.junit.Test

class LoginErrorMapperTest {

    @Test
    fun `every login error maps to its own message`() {
        assertEquals(R.string.error_login_invalid_format, LoginError.InvalidTokenFormat.toMessageRes())
        assertEquals(R.string.error_login_session_expired, LoginError.SessionExpired.toMessageRes())
        assertEquals(R.string.error_login_invalid_passport, LoginError.InvalidPassport.toMessageRes())
        assertEquals(R.string.error_login_invalid_token, LoginError.InvalidAccessToken.toMessageRes())
        assertEquals(R.string.error_login_process_failed, LoginError.TokenProcessingFailed.toMessageRes())
    }

    @Test
    fun `validation failures keep one message whatever the data error`() {
        listOf(DataError.Network, DataError.Unauthorized, DataError.Server("x"), DataError.Unknown).forEach { cause ->
            assertEquals(
                R.string.error_login_validation_failed,
                LoginError.ValidationFailed(cause).toMessageRes()
            )
        }
    }
}
