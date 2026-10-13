package it.polimi.webeep.core.util

import it.polimi.webeep.R
import it.polimi.webeep.core.datastore.MissingTokenException
import it.polimi.webeep.core.network.MoodleException
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import org.junit.Assert.assertEquals
import org.junit.Test

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
    fun `local file failures map to Storage`() {
        assertEquals(DataError.Storage, FileSystemException(File("x"), reason = "denied").toDataError())
        assertEquals(DataError.Storage, IOException("write failed: ENOSPC (No space left on device)").toDataError())
        assertEquals(DataError.Storage, IOException("No space left on device").toDataError())
    }

    @Test
    fun `file not found stays generic because url stacks throw it for remote resources`() {
        assertEquals(DataError.Network, FileNotFoundException("https://webeep.polimi.it/missing").toDataError())
    }

    @Test
    fun `moodle errors are never mistaken for storage failures`() {
        assertEquals(
            DataError.Server("No space left"),
            MoodleException("No space left", "quota").toDataError()
        )
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
        assertEquals(R.string.error_data_storage, DataError.Storage.toMessageRes())
        assertEquals(R.string.error_data_unknown, DataError.Unknown.toMessageRes())
    }
}
