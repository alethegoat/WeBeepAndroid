package it.polimi.webeep.core.datastore

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Only the file bookkeeping is covered: decrypting the legacy prefs needs the real Android Keystore.
 */
@RunWith(RobolectricTestRunner::class)
class LegacyEncryptedPrefsMigrationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val prefsName = "webeep_secure_prefs"

    @Before
    fun cleanPrefs() {
        context.deleteSharedPreferences(prefsName)
    }

    @Test
    fun `exists is false on a fresh install`() {
        assertFalse(LegacyEncryptedPrefsMigration.exists(context))
    }

    @Test
    fun `exists is true once the legacy prefs file is present`() {
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE).edit().putString("k", "v").commit()

        assertTrue(LegacyEncryptedPrefsMigration.exists(context))
    }

    @Test
    fun `delete removes the legacy prefs file`() {
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE).edit().putString("k", "v").commit()
        assertTrue(File(context.dataDir, "shared_prefs/$prefsName.xml").exists())

        LegacyEncryptedPrefsMigration.delete(context)

        assertFalse(LegacyEncryptedPrefsMigration.exists(context))
    }

    @Test
    fun `delete is harmless when nothing exists`() {
        LegacyEncryptedPrefsMigration.delete(context)

        assertFalse(LegacyEncryptedPrefsMigration.exists(context))
    }

    @Test
    fun `read discards prefs that cannot be decrypted`() {
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE).edit().putString("ws_token", "plain").commit()

        // Without a Keystore-backed master key the legacy file is unreadable and yields null (or no token).
        val record = LegacyEncryptedPrefsMigration.read(context)

        assertNull(record?.token)
    }
}
