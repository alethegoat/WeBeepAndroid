package it.polimi.webeep.core.util

import it.polimi.webeep.R
import it.polimi.webeep.core.datastore.MissingTokenException
import it.polimi.webeep.core.network.MoodleException
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class DataErrorMapperTest {

    @Test
    fun `io failures map to Network`() {
        assertEquals(DataError.Network, IOException("reset").toDataError())
        assertEquals(DataError.Network, UnknownHostException("webeep.polimi.it").toDataError())
        assertEquals(DataError.Network, SocketTimeoutException("timeout").toDataError())
    }

    @Test
    fun `invalid token moodle errors map to Unauthorized`() {
        assertEquals(DataError.Unauthorized, MoodleException("invalidtoken", "Invalid token").toDataError())
        assertEquals(DataError.Unauthorized, MoodleException("accessexception", "Access denied").toDataError())
    }

    @Test
    fun `other moodle errors map to Server with the error code`() {
        assertEquals(
            DataError.Server("invalidparameter"),
            MoodleException("invalidparameter", "Invalid parameter value").toDataError()
        )
    }

    @Test
    fun `missing token maps to Unauthorized`() {
        assertEquals(DataError.Unauthorized, MissingTokenException().toDataError())
    }

    @Test
    fun `unrelated failures map to Unknown`() {
        assertEquals(DataError.Unknown, IllegalStateException("Empty response").toDataError())
        assertEquals(DataError.Unknown, RuntimeException("boom").toDataError())
    }

    @Test
    fun `every error has a localized message`() {
        assertEquals(R.string.error_data_network, DataError.Network.toMessageRes())
        assertEquals(R.string.error_session_expired, DataError.Unauthorized.toMessageRes())
        assertEquals(R.string.error_data_server, DataError.Server("x").toMessageRes())
        assertEquals(R.string.error_data_unknown, DataError.Unknown.toMessageRes())
    }
}
