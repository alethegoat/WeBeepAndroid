package it.polimi.webeep.feature.sync.data

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import it.polimi.webeep.R
import it.polimi.webeep.feature.sync.domain.SyncExtras
import it.polimi.webeep.feature.sync.domain.model.ChangeType
import it.polimi.webeep.feature.sync.domain.model.CourseChanges
import it.polimi.webeep.feature.sync.domain.model.FileChange
import it.polimi.webeep.util.fakes.FakeSyncStateRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class SyncNotifierTest {

    private lateinit var app: Application
    private lateinit var manager: NotificationManager
    private lateinit var state: FakeSyncStateRepository
    private lateinit var notifier: SyncNotifier

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        manager = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        state = FakeSyncStateRepository()
        notifier = SyncNotifier(app, state)
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun added(name: String) = FileChange(1, "/", name, ChangeType.ADDED)

    private fun updated(name: String) = FileChange(1, "/", name, ChangeType.UPDATED)

    private fun course(id: Int, vararg changes: FileChange) = CourseChanges(id, "Course $id", changes.toList())

    private fun undeliver(vararg courses: CourseChanges) {
        state.state = state.state.copy(undelivered = courses.toList())
    }

    private fun active() = manager.activeNotifications.associateBy { it.id }

    private fun Notification.lines(): List<String> =
        extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES).orEmpty().map { it.toString() }

    @Test
    fun `createChannel registers the sync channel`() {
        notifier.createChannel()

        val channel = manager.getNotificationChannel(SyncNotifier.CHANNEL_ID)
        assertNotNull(channel)
        assertEquals(app.getString(R.string.sync_channel_name), channel.name.toString())
    }

    @Test
    fun `a single course posts one notification and no summary`() = runTest {
        undeliver(course(7, added("a.pdf")))

        notifier.deliverPending()

        assertEquals(setOf(7), active().keys)
        assertTrue(active().getValue(7).notification.flags and Notification.FLAG_GROUP_SUMMARY == 0)
    }

    @Test
    fun `several courses post one notification each plus a group summary`() = runTest {
        undeliver(course(7, added("a.pdf")), course(8, added("b.pdf")))

        notifier.deliverPending()

        val posted = active()
        assertEquals(setOf(7, 8, Int.MAX_VALUE), posted.keys)
        val summary = posted.getValue(Int.MAX_VALUE).notification
        assertTrue(summary.flags and Notification.FLAG_GROUP_SUMMARY != 0)
        assertEquals(posted.getValue(7).notification.group, summary.group)
        assertEquals(posted.getValue(8).notification.group, summary.group)
        assertEquals(
            app.getString(R.string.sync_completed_title),
            summary.extras.getString(Notification.EXTRA_TITLE)
        )
    }

    @Test
    fun `course notification lists new files before updated ones`() = runTest {
        undeliver(course(7, updated("old.pdf"), added("new.pdf")))

        notifier.deliverPending()

        assertEquals(
            listOf(
                app.getString(R.string.sync_line_added, "new.pdf"),
                app.getString(R.string.sync_line_updated, "old.pdf")
            ),
            active().getValue(7).notification.lines()
        )
    }

    @Test
    fun `course notification is capped at five lines with a summary of the rest`() = runTest {
        undeliver(course(7, *(1..7).map { added("f$it.pdf") }.toTypedArray()))

        notifier.deliverPending()

        val notification = active().getValue(7).notification
        assertEquals(5, notification.lines().size)
        assertEquals(app.getString(R.string.sync_more_files, 2), notification.extras.getString(Notification.EXTRA_SUMMARY_TEXT))
    }

    @Test
    fun `an unread notification accumulates the files of a later sync`() = runTest {
        undeliver(course(7, added("a.pdf")))
        notifier.deliverPending()

        undeliver(course(7, added("b.pdf")))
        notifier.deliverPending()

        assertEquals(
            setOf(
                app.getString(R.string.sync_line_added, "a.pdf"),
                app.getString(R.string.sync_line_added, "b.pdf")
            ),
            active().getValue(7).notification.lines().toSet()
        )
    }

    @Test
    fun `a dismissed notification does not carry its old files into the next one`() = runTest {
        undeliver(course(7, added("a.pdf")))
        notifier.deliverPending()
        manager.cancel(7)

        undeliver(course(7, added("b.pdf")))
        notifier.deliverPending()

        assertEquals(
            listOf(app.getString(R.string.sync_line_added, "b.pdf")),
            active().getValue(7).notification.lines()
        )
    }

    @Test
    fun `delivered changes are moved from undelivered to pending`() = runTest {
        undeliver(course(7, added("a.pdf")))

        notifier.deliverPending()

        assertTrue(state.state.undelivered.isEmpty())
        assertEquals(listOf(7), state.state.pending.map { it.courseId })
    }

    @Test
    fun `notifications are private on the lock screen with a public version without names`() = runTest {
        undeliver(course(7, added("secret.pdf")), course(8, added("other.pdf")))

        notifier.deliverPending()

        active().values.map { it.notification }.forEach { notification ->
            assertEquals(Notification.VISIBILITY_PRIVATE, notification.visibility)
            val public = notification.publicVersion
            assertNotNull(public)
            assertEquals(
                app.getString(R.string.sync_completed_title),
                public.extras.getString(Notification.EXTRA_TITLE)
            )
            val publicText = listOf(Notification.EXTRA_TITLE, Notification.EXTRA_TEXT, Notification.EXTRA_BIG_TEXT)
                .mapNotNull { public.extras.getCharSequence(it) }
                .joinToString()
            assertTrue(!publicText.contains("secret.pdf") && !publicText.contains("Course 7"))
        }
    }

    @Test
    fun `tapping a course notification opens the app with the course id`() = runTest {
        undeliver(course(7, added("a.pdf")), course(8, added("b.pdf")))

        notifier.deliverPending()

        val intent = shadowOf(active().getValue(8).notification.contentIntent).savedIntent
        assertEquals(8, intent.getIntExtra(SyncExtras.COURSE_ID, -1))
        assertEquals("EXTRA_COURSE_ID", SyncExtras.COURSE_ID)
    }

    @Test
    fun `tapping the summary opens the app without a course id`() = runTest {
        undeliver(course(7, added("a.pdf")), course(8, added("b.pdf")))

        notifier.deliverPending()

        val intent = shadowOf(active().getValue(Int.MAX_VALUE).notification.contentIntent).savedIntent
        assertTrue(!intent.hasExtra(SyncExtras.COURSE_ID))
    }

    @Test
    fun `nothing is posted and changes are kept when the permission is missing`() = runTest {
        shadowOf(app).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        undeliver(course(7, added("a.pdf")))

        notifier.deliverPending()

        assertTrue(manager.activeNotifications.isEmpty())
        assertEquals(listOf(7), state.state.undelivered.map { it.courseId })
    }

    @Test
    fun `nothing is posted when notifications are disabled for the app`() = runTest {
        shadowOf(manager).setNotificationsEnabled(false)
        undeliver(course(7, added("a.pdf")))

        notifier.deliverPending()

        assertTrue(manager.activeNotifications.isEmpty())
        assertEquals(1, state.state.undelivered.size)
    }

    @Test
    fun `nothing is posted when there are no undelivered changes`() = runTest {
        notifier.deliverPending()

        assertTrue(manager.activeNotifications.isEmpty())
    }

    @Test
    fun `the summary is cancelled when only one course remains on screen`() = runTest {
        undeliver(course(7, added("a.pdf")), course(8, added("b.pdf")))
        notifier.deliverPending()
        manager.cancel(7)
        manager.cancel(Int.MAX_VALUE)
        manager.cancel(8)

        undeliver(course(9, added("c.pdf")))
        notifier.deliverPending()

        assertEquals(setOf(9), active().keys)
    }

    @Test
    fun `cancelAll removes every sync notification`() = runTest {
        undeliver(course(7, added("a.pdf")), course(8, added("b.pdf")))
        notifier.deliverPending()

        notifier.cancelAll()

        assertTrue(manager.activeNotifications.isEmpty())
    }
}
