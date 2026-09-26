package it.polimi.webeep.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.IOException

/** Error envelope returned by Moodle web services (usually with HTTP 200). */
@Serializable
data class MoodleErrorDto(
    @SerialName("exception") val exception: String? = null,
    @SerialName("errorcode") val errorcode: String? = null,
    @SerialName("message") val message: String? = null
)

class MoodleException(val errorCode: String, message: String?) : IOException(message)
