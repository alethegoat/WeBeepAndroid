package it.polimi.webeep.feature.sync

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import it.polimi.webeep.MainActivity
import it.polimi.webeep.R
import it.polimi.webeep.feature.sync.FileChangeDetector.ChangeType
import it.polimi.webeep.feature.sync.FileChangeDetector.FileChange

/**
 * Posts "new / updated files" notifications, one per course plus a group summary.
 * Changes the user has not dismissed yet are carried over, so a later sync adds to a
 * course notification instead of replacing what it listed.
 */
object SyncNotifier {

    const val CHANNEL_ID = "webeep_sync_channel"
    private const val GROUP_KEY = "WE_BEEP_SYNC_GROUP"
    private const val SUMMARY_NOTIFICATION_ID = Int.MAX_VALUE
    private const val SUMMARY_REQUEST_CODE = -1
    private const val MAX_INBOX_LINES = 5

    /** Safe to call repeatedly; registering early lets users tune the channel before the first alert. */
    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.sync_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.sync_channel_desc)
        }
        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }

    /**
     * Posts every change the sync detected but has not notified yet. When notifications cannot
     * be shown the changes stay stored and are delivered by a later call.
     */
    suspend fun deliverPending(context: Context, stateStore: SyncStateStore) {
        if (!canPostNotifications(context)) return

        createChannel(context)
        val activeIds = activeNotificationIds(context)
        val (shown, freshIds) = stateStore.update { state ->
            if (state.undelivered.isEmpty()) return@update state to (emptyList<CourseChanges>() to emptySet<Int>())
            val merged = SyncStateStore.mergePending(state.pending, activeIds, state.undelivered)
            state.copy(pending = merged, undelivered = emptyList()) to (merged to state.undelivered.map { it.courseId }.toSet())
        }
        if (freshIds.isEmpty()) return

        val manager = NotificationManagerCompat.from(context)
        val hasSummary = shown.size > 1
        try {
            // Courses without news keep their current notification untouched (and silent).
            shown.filter { it.courseId in freshIds }.forEach { course ->
                manager.notify(course.courseId, buildCourseNotification(context, course, hasSummary))
            }
            if (hasSummary) {
                manager.notify(SUMMARY_NOTIFICATION_ID, buildSummaryNotification(context, shown))
            } else {
                manager.cancel(SUMMARY_NOTIFICATION_ID)
            }
        } catch (e: SecurityException) {
            // Permission revoked between the check and the post.
            Log.w("SyncNotifier", "Notification permission revoked type=${e::class.simpleName}")
        }
    }

    /** Removes every sync notification, e.g. on logout so no course or file name is left behind. */
    fun cancelAll(context: Context) {
        val manager = NotificationManagerCompat.from(context)
        activeNotificationIds(context).forEach { manager.cancel(it) }
    }

    private fun activeNotificationIds(context: Context): Set<Int> =
        context.getSystemService(NotificationManager::class.java)
            ?.activeNotifications
            ?.filter { it.notification.channelId == CHANNEL_ID }
            ?.map { it.id }
            ?.toSet()
            .orEmpty()

    private fun canPostNotifications(context: Context): Boolean {
        val permissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return permissionGranted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    private fun buildCourseNotification(context: Context, course: CourseChanges, isGrouped: Boolean): Notification {
        val inboxStyle = NotificationCompat.InboxStyle().setBigContentTitle(course.courseName)
        // New files first: they are what the user most likely cares about.
        val ordered = course.changes.sortedBy { it.type != ChangeType.ADDED }
        ordered.take(MAX_INBOX_LINES).forEach { change ->
            val line = when (change.type) {
                ChangeType.ADDED -> context.getString(R.string.sync_line_added, change.filename)
                ChangeType.UPDATED -> context.getString(R.string.sync_line_updated, change.filename)
            }
            inboxStyle.addLine(line)
        }
        if (ordered.size > MAX_INBOX_LINES) {
            inboxStyle.setSummaryText(context.getString(R.string.sync_more_files, ordered.size - MAX_INBOX_LINES))
        }

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setContentTitle(course.courseName)
            .setContentText(countsText(context, course.changes))
            .setStyle(inboxStyle)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(openAppIntent(context, course.courseId, course.courseId))
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(buildPublicNotification(context))
            .setGroup(GROUP_KEY)
            // With a summary, only the summary makes a sound: one alert per sync, not one per course.
            .setGroupAlertBehavior(
                if (isGrouped) NotificationCompat.GROUP_ALERT_SUMMARY else NotificationCompat.GROUP_ALERT_ALL
            )
            .setAutoCancel(true)
            .build()
    }

    private fun buildSummaryNotification(context: Context, courses: List<CourseChanges>): Notification {
        val allChanges = courses.flatMap { it.changes }
        val countsText = countsText(context, allChanges)
        val inboxStyle = NotificationCompat.InboxStyle()
            .setBigContentTitle(context.getString(R.string.sync_completed_title))
            .setSummaryText(countsText)
        courses.take(MAX_INBOX_LINES).forEach { course ->
            inboxStyle.addLine(
                context.getString(R.string.sync_summary_line, course.courseName, countsText(context, course.changes))
            )
        }

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setContentTitle(context.getString(R.string.sync_completed_title))
            .setContentText(countsText)
            .setStyle(inboxStyle)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(openAppIntent(context, courseId = null, requestCode = SUMMARY_REQUEST_CODE))
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(buildPublicNotification(context))
            .setGroup(GROUP_KEY)
            .setGroupSummary(true)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
            .setAutoCancel(true)
            .build()
    }

    private fun countsText(context: Context, changes: List<FileChange>): String {
        val added = changes.count { it.type == ChangeType.ADDED }
        val updated = changes.size - added
        val parts = buildList {
            if (added > 0) add(context.resources.getQuantityString(R.plurals.sync_new_files, added, added))
            if (updated > 0) add(context.resources.getQuantityString(R.plurals.sync_updated_files, updated, updated))
        }
        return parts.joinToString(" · ")
    }

    private fun openAppIntent(context: Context, courseId: Int?, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            courseId?.let { putExtra(CourseSyncWorker.EXTRA_COURSE_ID, it) }
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Lock-screen variant: no course or file names. */
    private fun buildPublicNotification(context: Context) =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setContentTitle(context.getString(R.string.sync_completed_title))
            .build()
}
