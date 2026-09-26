package it.polimi.webeep.feature.sync

import android.Manifest
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
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import it.polimi.webeep.MainActivity
import it.polimi.webeep.R
import it.polimi.webeep.core.database.dao.CourseDao
import it.polimi.webeep.core.datastore.TokenProvider
import it.polimi.webeep.feature.course.data.CourseContentMapper
import it.polimi.webeep.feature.course.domain.CourseRepository
import it.polimi.webeep.feature.course.domain.model.Section
import it.polimi.webeep.feature.dashboard.domain.DashboardRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.atomic.AtomicBoolean

@HiltWorker
class CourseSyncWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val courseDao: CourseDao,
    private val courseRepository: CourseRepository,
    private val dashboardRepository: DashboardRepository,
    private val tokenProvider: TokenProvider
) : CoroutineWorker(appContext, workerParams) {

    data class NewFileRecord(val courseId: Int, val courseName: String, val fileName: String)

    override suspend fun doWork(): Result {
        Log.d("CourseSyncWorker", "Worker started")
        tokenProvider.getToken() ?: run {
            Log.d("CourseSyncWorker", "No token. Stopping worker.")
            return Result.failure()
        }

        try {
            dashboardRepository.syncCourses()

            val courses = courseDao.getAllCourses().firstOrNull()
            if (courses.isNullOrEmpty()) {
                Log.d("CourseSyncWorker", "No local courses. Stopping worker.")
                return Result.success()
            }

            var totalNewFiles = 0
            val newFileRecords = mutableListOf<NewFileRecord>()
            val hasErrors = AtomicBoolean(false)
            val syncedCourses = java.util.concurrent.atomic.AtomicInteger(0)
            val courseSyncSemaphore = Semaphore(MAX_CONCURRENT_COURSE_SYNCS)

            supervisorScope {
                val jobs = courses.map { course ->
                    async {
                        courseSyncSemaphore.withPermit {
                            try {
                                val oldKeys = fileIdentitiesFrom(courseRepository.getCachedCourseContents(course.id).first())
                                    .map { it.key }
                                    .toSet()
                                val syncResult = courseRepository.getCourseContents(course.id)
                                val newFiles = fileIdentitiesFrom(syncResult.getOrElse { throw it })
                                syncedCourses.incrementAndGet()

                                if (oldKeys.isNotEmpty()) {
                                    val added = newFiles.filter { it.key !in oldKeys }
                                    if (added.isNotEmpty()) {
                                        val records = added.map { file ->
                                            NewFileRecord(course.id, course.fullname, file.filename)
                                        }
                                        synchronized(newFileRecords) {
                                            totalNewFiles += records.size
                                            newFileRecords.addAll(records)
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                if (e is kotlinx.coroutines.CancellationException) throw e
                                Log.e("CourseSyncWorker", "Sync error course=${course.id} type=${e::class.simpleName}")
                                hasErrors.set(true)
                            }
                        }
                    }
                }
                jobs.awaitAll()
            }

            if (totalNewFiles > 0) {
                createNotificationChannel()
                val filesByCourse = newFileRecords.groupBy { it.courseId }
                if (filesByCourse.size > 1) {
                    sendSummaryNotification(totalNewFiles)
                }
                filesByCourse.forEach { (courseId, records) ->
                    val courseName = records.first().courseName
                    sendNotification(courseId, courseName, records)
                }
            }

            // Avoid retry loops: partial success is enough since the next
            // periodic run will pick up the rest. Retry only when nothing synced.
            return if (hasErrors.get() && syncedCourses.get() == 0) Result.retry() else Result.success()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("CourseSyncWorker", "Fatal worker error type=${e::class.simpleName}")
            return Result.retry()
        }
    }

    private data class FileIdentity(val key: String, val filename: String)

    private fun fileIdentitiesFrom(sections: List<Section>): List<FileIdentity> {
        return sections.flatMap { section ->
            section.modules.flatMap { module ->
                module.files.map { file ->
                    FileIdentity(
                        key = CourseContentMapper.fileKey(file.fileUrl, file.filename, file.timeModified),
                        filename = file.filename
                    )
                }
            }
        }
    }

    private fun createNotificationChannel() {
        val channelId = "webeep_sync_channel"
        val notificationManager = NotificationManagerCompat.from(appContext)
        val channel = NotificationChannel(
            channelId,
            appContext.getString(R.string.sync_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = appContext.getString(R.string.sync_channel_desc)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun sendSummaryNotification(totalNewFiles: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val channelId = "webeep_sync_channel"
        val notificationManager = NotificationManagerCompat.from(appContext)

        val summaryNotification = NotificationCompat.Builder(appContext, channelId)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setContentTitle(appContext.getString(R.string.sync_completed_title))
            .setContentText(appContext.getString(R.string.sync_completed, totalNewFiles))
            .setStyle(
                NotificationCompat.InboxStyle()
                    .setBigContentTitle(appContext.getString(R.string.sync_completed_title))
                    .setSummaryText(appContext.getString(R.string.sync_summary, totalNewFiles))
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(buildPublicNotification(channelId))
            .setGroup("WE_BEEP_SYNC_GROUP")
            .setGroupSummary(true)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(SUMMARY_NOTIFICATION_ID, summaryNotification)
    }

    private fun sendNotification(courseId: Int, courseName: String, fileRecords: List<NewFileRecord>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val channelId = "webeep_sync_channel"
        val notificationManager = NotificationManagerCompat.from(appContext)

        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_COURSE_ID, courseId)
        }

        val pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent: PendingIntent = PendingIntent.getActivity(appContext, courseId, intent, pendingIntentFlags)

        val inboxStyle = NotificationCompat.InboxStyle()
        inboxStyle.setBigContentTitle(courseName)
        fileRecords.take(MAX_INBOX_LINES).forEach { record ->
            inboxStyle.addLine(record.fileName)
        }
        if (fileRecords.size > MAX_INBOX_LINES) {
            inboxStyle.setSummaryText(appContext.getString(R.string.sync_more_files, fileRecords.size - MAX_INBOX_LINES))
        }

        val builder = NotificationCompat.Builder(appContext, channelId)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setContentTitle(courseName)
            .setContentText(appContext.getString(R.string.sync_summary, fileRecords.size))
            .setStyle(inboxStyle)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(buildPublicNotification(channelId))
            .setGroup("WE_BEEP_SYNC_GROUP")
            .setAutoCancel(true)

        notificationManager.notify(courseId, builder.build())
    }

    /**
     * Lock-screen variant: no course or file names.
     */
    private fun buildPublicNotification(channelId: String) =
        NotificationCompat.Builder(appContext, channelId)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setContentTitle(appContext.getString(R.string.sync_completed_title))
            .build()

    companion object {
        const val EXTRA_COURSE_ID = "EXTRA_COURSE_ID"
        private const val MAX_CONCURRENT_COURSE_SYNCS = 2
        private const val SUMMARY_NOTIFICATION_ID = 999_999
        private const val MAX_INBOX_LINES = 5
    }
}
