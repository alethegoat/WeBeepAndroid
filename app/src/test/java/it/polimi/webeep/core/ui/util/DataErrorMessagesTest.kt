package it.polimi.webeep.core.ui.util

import it.polimi.webeep.R
import it.polimi.webeep.core.common.DataError
import org.junit.Assert.assertEquals
import org.junit.Test

class DataErrorMessagesTest {

    @Test
    fun `every error has a localized message`() {
        assertEquals(R.string.error_data_network, DataError.Network.toMessageRes())
        assertEquals(R.string.error_session_expired, DataError.Unauthorized.toMessageRes())
        assertEquals(R.string.error_data_server, DataError.Server("x").toMessageRes())
        assertEquals(R.string.error_data_storage, DataError.Storage.toMessageRes())
        assertEquals(R.string.error_data_unknown, DataError.Unknown.toMessageRes())
    }
}
