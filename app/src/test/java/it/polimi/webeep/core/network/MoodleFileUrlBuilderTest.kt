package it.polimi.webeep.core.network

import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
class MoodleFileUrlBuilderTest {

    @Before
    fun setup() {
        mockkStatic(Uri::class)
        every { Uri.parse(any()) } answers {
            val urlString = firstArg<String>()
            val isHttps = urlString.startsWith("https", ignoreCase = true)
            val isPolimi = urlString.contains("webeep.polimi.it", ignoreCase = true)
            
            mockk<Uri> {
                every { scheme } returns if (isHttps) "https" else "http"
                every { host } returns if (isPolimi) "webeep.polimi.it" else "other.com"
            }
        }
    }

    @Test
    fun `buildAuthenticatedUrl converts pluginfile url and appends token`() {
        val result = MoodleFileUrlBuilder.buildAuthenticatedUrl(
            url = "https://webeep.polimi.it/pluginfile.php/123/mod_resource/content/1/file.pdf",
            token = "abc123"
        )

        assertEquals(
            "https://webeep.polimi.it/webservice/pluginfile.php/123/mod_resource/content/1/file.pdf?token=abc123",
            result
        )
    }

    @Test
    fun `buildAuthenticatedUrl uses ampersand when query parameters already exist`() {
        val result = MoodleFileUrlBuilder.buildAuthenticatedUrl(
            url = "https://webeep.polimi.it/webservice/pluginfile.php/123/file.pdf?forcedownload=1",
            token = "abc123"
        )

        assertEquals(
            "https://webeep.polimi.it/webservice/pluginfile.php/123/file.pdf?forcedownload=1&token=abc123",
            result
        )
    }

    @Test
    fun `stripAuthFromUrl removes token query parameters`() {
        val result = MoodleFileUrlBuilder.stripAuthFromUrl(
            "https://webeep.polimi.it/webservice/pluginfile.php/1/file.pdf?token=secret&forcedownload=1"
        )

        assertEquals(
            "https://webeep.polimi.it/webservice/pluginfile.php/1/file.pdf?forcedownload=1",
            result
        )
    }

    @Test
    fun `buildAuthenticatedUrl does not append duplicate token`() {
        val result = MoodleFileUrlBuilder.buildAuthenticatedUrl(
            url = "https://webeep.polimi.it/webservice/pluginfile.php/123/file.pdf?token=abc123",
            token = "other"
        )

        assertEquals(
            "https://webeep.polimi.it/webservice/pluginfile.php/123/file.pdf?token=abc123",
            result
        )
    }
}
