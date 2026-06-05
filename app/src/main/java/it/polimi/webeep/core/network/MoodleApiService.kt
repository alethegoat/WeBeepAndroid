package it.polimi.webeep.core.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import it.polimi.webeep.core.network.dto.CourseDto
import it.polimi.webeep.core.network.dto.CourseSectionDto
import it.polimi.webeep.core.network.dto.SiteInfoDto
import it.polimi.webeep.core.network.dto.TimelineCoursesResponseDto

/**
 * interface for moodle web service api calls.
 */
interface MoodleApiService {

    /**
     * retrieves site and authenticated user information.
     */
    suspend fun getSiteInfo(wsToken: String? = null): SiteInfoDto

    /**
     * retrieves the list of courses the user is enrolled in.
     */
    suspend fun getEnrolledCourses(wsToken: String? = null, userId: Int): List<CourseDto>

    /**
     * retrieves courses classified by timeline (inprogress, past, future).
     */
    suspend fun getCoursesByTimeline(
        classification: String = "inprogress",
        limit: Int = 0,
        offset: Int = 0
    ): TimelineCoursesResponseDto

    /**
     * retrieves course contents (sections, modules and files).
     */
    suspend fun getCourseContents(
        wsToken: String? = null,
        courseId: Int
    ): List<CourseSectionDto>
}

class MoodleApiServiceImpl(private val client: HttpClient) : MoodleApiService {
    
    private val serverPath = "webservice/rest/server.php"
    private val defaultFormat = "json"

    override suspend fun getSiteInfo(wsToken: String?): SiteInfoDto {
        return client.get(serverPath) {
            wsToken?.let { parameter("wstoken", it) }
            parameter("wsfunction", "core_webservice_get_site_info")
            parameter("moodlewsrestformat", defaultFormat)
        }.body()
    }

    override suspend fun getEnrolledCourses(wsToken: String?, userId: Int): List<CourseDto> {
        return client.get(serverPath) {
            wsToken?.let { parameter("wstoken", it) }
            parameter("wsfunction", "core_enrol_get_users_courses")
            parameter("userid", userId)
            parameter("moodlewsrestformat", defaultFormat)
        }.body()
    }

    override suspend fun getCoursesByTimeline(
        classification: String,
        limit: Int,
        offset: Int
    ): TimelineCoursesResponseDto {
        return client.get(serverPath) {
            parameter("wsfunction", "core_course_get_enrolled_courses_by_timeline_classification")
            parameter("classification", classification)
            parameter("limit", limit)
            parameter("offset", offset)
            parameter("moodlewsrestformat", defaultFormat)
        }.body()
    }

    override suspend fun getCourseContents(wsToken: String?, courseId: Int): List<CourseSectionDto> {
        return client.get(serverPath) {
            wsToken?.let { parameter("wstoken", it) }
            parameter("wsfunction", "core_course_get_contents")
            parameter("courseid", courseId)
            parameter("moodlewsrestformat", defaultFormat)
        }.body()
    }
}
