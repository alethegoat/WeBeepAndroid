package it.polimi.webeep.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MoodleFileUrlBuilderTest {

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

    @Test
    fun `buildAuthenticatedUrl does not append token to suffix lookalike hosts`() {
        val result = MoodleFileUrlBuilder.buildAuthenticatedUrl(
            url = "https://attackerpolimi.it/webservice/pluginfile.php/123/file.pdf",
            token = "secret"
        )
        assertFalse(result.contains("token="))
    }

    @Test
    fun `buildAuthenticatedUrl does not append token to other polimi subdomains`() {
        val result = MoodleFileUrlBuilder.buildAuthenticatedUrl(
            url = "https://www.polimi.it/webservice/pluginfile.php/123/file.pdf",
            token = "secret"
        )
        assertFalse(result.contains("token="))
    }

    @Test
    fun `buildAuthenticatedUrl does not append token to non pluginfile webeep pages`() {
        val result = MoodleFileUrlBuilder.buildAuthenticatedUrl(
            url = "https://webeep.polimi.it/mod/url/view.php?id=5",
            token = "secret"
        )
        assertEquals("https://webeep.polimi.it/mod/url/view.php?id=5", result)
    }

    @Test
    fun `trusted host helper is exact match`() {
        assertTrue(MoodleFileUrlBuilder.isTrustedHost("webeep.polimi.it"))
        assertTrue(MoodleFileUrlBuilder.isTrustedHost("WeBeep.Polimi.IT"))
        assertFalse(MoodleFileUrlBuilder.isTrustedHost("attackerpolimi.it"))
        assertFalse(MoodleFileUrlBuilder.isTrustedHost("evil.polimi.it"))
    }

    @Test
    fun `isExternalWebLink is true only for http links that are not trusted pluginfiles`() {
        assertTrue(MoodleFileUrlBuilder.isExternalWebLink("https://example.com/slides"))
        assertTrue(MoodleFileUrlBuilder.isExternalWebLink("https://webeep.polimi.it/mod/url/view.php?id=1"))
        assertFalse(
            MoodleFileUrlBuilder.isExternalWebLink(
                "https://webeep.polimi.it/pluginfile.php/123/mod_resource/content/1/file.pdf"
            )
        )
        assertFalse(MoodleFileUrlBuilder.isExternalWebLink("mailto:someone@polimi.it"))
    }
}
