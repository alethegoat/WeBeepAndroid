package it.polimi.webeep.core.network

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ParametersBuilder
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import it.polimi.webeep.core.common.MoodleException
import it.polimi.webeep.core.network.dto.CourseDto
import it.polimi.webeep.core.network.dto.CourseSectionDto
import it.polimi.webeep.core.network.dto.SiteInfoDto
import java.io.IOException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

/**
 * Moodle web-service calls used by the app.
 *
 * A `null` `wsToken` means "use the saved session token" (added by [TokenInterceptor]); an explicit value is
 * only for validating a token before it is saved (login). Failures surface as [MoodleException] (application
 * error), `IOException` (transport or HTTP) or `SerializationException` (unexpected payload).
 */
interface MoodleApiService {

    /** Fetches the profile behind the token; throws [IllegalArgumentException] if the response has no valid user id. */
    suspend fun getSiteInfo(wsToken: String? = null): SiteInfoDto

    /** Lists the courses [userId] is enrolled in; [userId] must be positive. */
    suspend fun getEnrolledCourses(wsToken: String? = null, userId: Int): List<CourseDto>

    /** Fetches the sections, modules and file contents of [courseId]. */
    suspend fun getCourseContents(wsToken: String? = null, courseId: Int): List<CourseSectionDto>
}

/**
 * Moodle REST client. Calls are POST forms so `wstoken` travels in the body,
 * never in the URL. The saved token is added by [TokenInterceptor] only for
 * the trusted host; an explicit `wsToken` is used for pre-login validation.
 */
class MoodleApiServiceImpl(private val client: HttpClient) : MoodleApiService {

    /** Request marker and endpoint constants shared with the interceptors. */
    companion object {
        /** Internal marker: the request is a read-only WS call, safe to retry. Stripped before sending. */
        const val READ_ONLY_WS_HEADER = "X-WeBeep-Read-Only-WS"

        private const val SERVER_PATH = "webservice/rest/server.php"
        private const val DEFAULT_FORMAT = "json"

        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }
    }

    override suspend fun getSiteInfo(wsToken: String?): SiteInfoDto {
        val siteInfo = ws<SiteInfoDto>("core_webservice_get_site_info", wsToken)
        require(siteInfo.userid > 0) { "Invalid site info" }
        return siteInfo
    }

    override suspend fun getEnrolledCourses(wsToken: String?, userId: Int): List<CourseDto> {
        require(userId > 0) { "Invalid user id" }
        return ws("core_enrol_get_users_courses", wsToken) {
            append("userid", userId.toString())
        }
    }

    override suspend fun getCourseContents(wsToken: String?, courseId: Int): List<CourseSectionDto> =
        ws("core_course_get_contents", wsToken) {
            append("courseid", courseId.toString())
        }

    private suspend inline fun <reified T> ws(
        function: String,
        explicitToken: String?,
        crossinline extra: ParametersBuilder.() -> Unit = {}
    ): T {
        val response = client.submitForm(
            url = SERVER_PATH,
            formParameters = parameters {
                explicitToken?.let { append("wstoken", it) }
                append("wsfunction", function)
                append("moodlewsrestformat", DEFAULT_FORMAT)
                extra()
            }
        ) {
            header(READ_ONLY_WS_HEADER, "1")
        }
        return json.decodeFromJsonElement(response.moodleJson())
    }

    // Maps transport, parse and Moodle-level failures to distinct exception types.
    @Suppress("ThrowsCount")
    private suspend fun HttpResponse.moodleJson(): JsonElement {
        val element = try {
            json.parseToJsonElement(bodyAsText())
        } catch (e: SerializationException) {
            if (!status.isSuccess()) throw IOException("HTTP ${status.value}")
            throw e
        }
        if (element is JsonObject && ("exception" in element || "errorcode" in element)) {
            val error = json.decodeFromJsonElement<MoodleErrorDto>(element)
            throw MoodleException(error.errorcode ?: "unknown", error.message)
        }
        if (!status.isSuccess()) throw IOException("HTTP ${status.value}")
        return element
    }
}
