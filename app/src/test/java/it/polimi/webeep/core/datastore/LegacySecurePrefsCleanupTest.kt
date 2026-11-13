package it.polimi.webeep.core.datastore

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Only the file is covered: the Keystore master key cleanup needs the real Android Keystore.
 */
@RunWith(RobolectricTestRunner::class)
class LegacySecurePrefsCleanupTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val prefsName = "webeep_secure_prefs"
    private val prefsFile = File(context.dataDir, "shared_prefs/$prefsName.xml")

    @Before
    fun cleanPrefs() {
        context.deleteSharedPreferences(prefsName)
    }

    @Test
    fun `run removes the legacy prefs file`() {
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE).edit().putString("k", "v").commit()
        assertTrue(prefsFile.exists())

        LegacySecurePrefsCleanup.run(context)

        assertFalse(prefsFile.exists())
    }

    @Test
    fun `run is harmless when nothing exists`() {
        LegacySecurePrefsCleanup.run(context)

        assertFalse(prefsFile.exists())
    }
}
