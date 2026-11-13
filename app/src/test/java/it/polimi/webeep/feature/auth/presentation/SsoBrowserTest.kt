package it.polimi.webeep.feature.auth.presentation

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SsoBrowserTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    /** Mimics a device where no activity resolves any intent, which is what throws in production. */
    private class NoBrowserContext(base: Context) : ContextWrapper(base) {
        override fun startActivity(intent: Intent?) = throw ActivityNotFoundException()

        override fun startActivity(intent: Intent?, options: Bundle?) = throw ActivityNotFoundException()
    }

    @Test
    fun `open reports failure when no browser can handle the link`() {
        assertFalse(SsoBrowser.open(NoBrowserContext(context), "https://example.com/login"))
    }

    @Test
    fun `open reports success when an activity is launched`() {
        assertTrue(SsoBrowser.open(context, "https://example.com/login"))
    }
}
