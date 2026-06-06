package it.polimi.webeep.feature.sync

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
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
import it.polimi.webeep.core.database.dao.CourseContentDao
import it.polimi.webeep.core.database.dao.CourseDao
import it.polimi.webeep.core.datastore.TokenManager
import it.polimi.webeep.core.network.MoodleApiService

import kotlinx.coroutines.flow.firstOrNull
import it.polimi.webeep.core.database.entity.CourseModuleEntity
import it.polimi.webeep.core.database.entity.CourseSectionEntity
import it.polimi.webeep.core.database.entity.ModuleContentEntity
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

import android.util.Log
import it.polimi.webeep.core.util.MoodleText

/**
 * worker for synchronizing course files in the background.
 */
@HiltWorker
class CourseSyncWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val moodleApiService: MoodleApiService,
    private val courseDao: CourseDao,
    private val courseContentDao: CourseContentDao,
    private val tokenManager: TokenManager
) : CoroutineWorker(appContext, workerParams) {

    data class NewFileRecord(val courseId: Int, val courseName: String, val fileName: String)

    override suspend fun doWork(): Result {
        Log.d("CourseSyncWorker", "🔄 Worker avviato: Inizio controllo nuovi file")
        val token = tokenManager.getToken() ?: run {
            Log.d("CourseSyncWorker", "❌ Token non trovato. Interruzione Worker.")
            return Result.failure()
        }

        try {
            val courses = courseDao.getAllCourses().firstOrNull()
            if (courses.isNullOrEmpty()) {
                Log.d("CourseSyncWorker", "⚠️ Nessun corso trovato nel DB locale. Interruzione Worker.")
                return Result.success()
            }
            Log.d("CourseSyncWorker", "📚 Trovati ${courses.size} corsi nel DB locale da controllare.")

            var totalNewFiles = 0
            val newFileRecords = mutableListOf<NewFileRecord>()
            var hasErrors = false

            // supervisorscope prevents a course failure from cancelling the processing of the others
            kotlinx.coroutines.supervisorScope {
                val jobs = courses.map { course ->
                    async {
                        try {
                            Log.d("CourseSyncWorker", "🔍 [${course.shortname}] Inizio controllo...")

                            // 1. fetch old content
                            val oldSections = courseContentDao.getSectionsByCourseSync(course.id)
                            val oldFilesKeys = mutableSetOf<String>()
                            
                            oldSections.forEach { sectionWithModules ->
                                sectionWithModules.modules.forEach { moduleWithContents ->
                                    moduleWithContents.contents.forEach { content ->
                                        // unique key: combine stripped url and filename to be immune to dynamic token changes
                                        val cleanUrl = it.polimi.webeep.core.network.MoodleFileUrlBuilder.stripAuthFromUrl(content.fileUrl)
                                        val uniqueKey = "${cleanUrl}_${content.filename}"
                                        oldFilesKeys.add(uniqueKey)
                                    }
                                }
                            }
                            Log.d("CourseSyncWorker", "📦 [${course.shortname}] Trovati ${oldFilesKeys.size} file nella cache locale.")

                            // 2. download new contents from api
                            val newSectionsDto = moodleApiService.getCourseContents(courseId = course.id)
                            
                            val sectionEntities = mutableListOf<CourseSectionEntity>()
                            val moduleEntities = mutableListOf<CourseModuleEntity>()
                            val contentEntities = mutableListOf<ModuleContentEntity>()
                            
                            val currentNewFiles = mutableListOf<NewFileRecord>()
                            var courseFilesCount = 0
                            
                            newSectionsDto.forEach { sectionDto ->
                                sectionEntities.add(
                                    CourseSectionEntity(
                                        id = sectionDto.id,
                                        courseId = course.id,
                                        name = MoodleText.parse(sectionDto.name),
                                        summary = MoodleText.parse(sectionDto.summary),
                                        section = sectionDto.section,
                                        visible = sectionDto.visible == 1 || sectionDto.uservisible == true
                                    )
                                )

                                sectionDto.modules.forEach { mod ->
                                    val directUrl = if (mod.modname in setOf("url", "lti", "webex")) {
                                        mod.contents?.firstOrNull { it.type == "url" || !it.fileurl.isNullOrBlank() }?.fileurl
                                    } else {
                                        null
                                    }
                                    val finalUrl = directUrl ?: mod.url

                                    moduleEntities.add(
                                        CourseModuleEntity(
                                            id = mod.id,
                                            sectionId = sectionDto.id,
                                            name = MoodleText.parse(mod.name),
                                            modName = mod.modname,
                                            iconUrl = mod.modicon,
                                            description = MoodleText.parse(mod.description),
                                            url = finalUrl
                                        )
                                    )

                                    // only process contents for modules that actually contain physical files
                                    val allowedFileModules = setOf("resource", "folder")
                                    if (mod.modname in allowedFileModules) {
                                        mod.contents?.forEach { content ->
                                            // ignore empty directories from moodle api
                                            if (content.type == "dir") return@forEach

                                            courseFilesCount++
                                            val filename = MoodleText.parse(content.filename ?: "Senza nome")
                                            val rawUrl = content.fileurl ?: ""
                                            val fileUrl = it.polimi.webeep.core.network.MoodleFileUrlBuilder.stripAuthFromUrl(rawUrl)
                                            val filepath = content.filepath
                                            
                                            val uniqueKey = "${fileUrl}_${filename}"

                                        // correct diffing: verify url+name.
                                        // adding isnotempty() prevents notification spam on absolute first launch.
                                        if (!oldFilesKeys.contains(uniqueKey) && oldFilesKeys.isNotEmpty()) {
                                            Log.d("CourseSyncWorker", "✨ [${course.shortname}] Trovato nuovo file: $filename")
                                            currentNewFiles.add(NewFileRecord(course.id, course.fullname, filename))
                                        }

                                            contentEntities.add(
                                                ModuleContentEntity(
                                                    moduleId = mod.id,
                                                    filename = filename,
                                                    filepath = filepath,
                                                    fileUrl = rawUrl, // save raw url to maintain any required tracking params, auth is handled by builder
                                                    fileSize = content.filesize,
                                                    mimeType = content.mimetype,
                                                    timeModified = content.timemodified
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                            Log.d("CourseSyncWorker", "🌐 [${course.shortname}] Scaricati $courseFilesCount file dalle API di Moodle.")

                            // 3. update the db for this course atomically
                            courseContentDao.replaceCourseContent(course.id, sectionEntities, moduleEntities, contentEntities)
                            
                            Log.d("CourseSyncWorker", "💾 [${course.shortname}] Database locale aggiornato con successo in transazione.")

                            // 4. only if the db was updated correctly do we save notifications
                            if (currentNewFiles.isNotEmpty()) {
                                synchronized(newFileRecords) {
                                    totalNewFiles += currentNewFiles.size
                                    newFileRecords.addAll(currentNewFiles)
                                }
                            }
                        } catch (e: Exception) {
                            if (e is kotlinx.coroutines.CancellationException) throw e
                            Log.e("CourseSyncWorker", "❌ [${course.shortname}] Errore durante il sync: ${e.message}")
                            hasErrors = true
                        }
                    }
                }
                jobs.awaitAll()
            }
            
            Log.d("CourseSyncWorker", "✅ Sync completato. File nuovi totali confermati: $totalNewFiles")
            
            if (totalNewFiles > 0) {
                Log.d("CourseSyncWorker", "🔔 Invio notifiche per i corsi con file nuovi.")
                
                // ensure the notification channel is created before sending anything
                createNotificationChannel()
                
                // group the new files by course so that we can send a distinct notification for each course.
                val filesByCourse = newFileRecords.groupBy { it.courseId }
                
                // create a summary notification if multiple courses have updates
                // this is required by android to visually bundle the notifications together.
                if (filesByCourse.size > 1) {
                    sendSummaryNotification(totalNewFiles)
                }

                // send individual notifications for each course containing new files.
                // each notification will have a unique id (the course id) and a dedicated deep link.
                filesByCourse.forEach { (courseId, records) ->
                    val courseName = records.first().courseName
                    sendNotification(courseId, courseName, records)
                }
            }

            return if (hasErrors) Result.retry() else Result.success()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("CourseSyncWorker", "💥 Errore fatale nel Worker: ${e.message}")
            e.printStackTrace()
            return Result.retry()
        }
    }

    private fun createNotificationChannel() {
        val channelId = "webeep_sync_channel"
        val notificationManager = NotificationManagerCompat.from(appContext)
        val channel = NotificationChannel(
            channelId,
            "Nuovi Documenti WeBeep",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifiche per i nuovi materiali caricati nei corsi"
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
            .setSmallIcon(android.R.drawable.sym_def_app_icon)
            .setContentTitle("Sincronizzazione completata")
            .setContentText(appContext.getString(R.string.sync_completed, totalNewFiles))
            .setSmallIcon(android.R.drawable.sym_def_app_icon)
            .setStyle(NotificationCompat.InboxStyle()
                .setBigContentTitle("Sincronizzazione completata")
                .setSummaryText(appContext.getString(R.string.sync_summary, totalNewFiles))
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setGroup("WE_BEEP_SYNC_GROUP")
            .setGroupSummary(true)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1000, summaryNotification) // summary id
    }

    private fun sendNotification(courseId: Int, courseName: String, fileRecords: List<NewFileRecord>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d("CourseSyncWorker", "🔕 Permesso notifiche non concesso.")
            return
        }

        val channelId = "webeep_sync_channel"
        val notificationManager = NotificationManagerCompat.from(appContext)

        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // attach the course id to the intent so that mainactivity can deep link directly to it
            putExtra("EXTRA_COURSE_ID", courseId)
        }
        
        val pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        
        // use courseid as requestcode so each course gets a mathematically distinct pendingintent
        val pendingIntent: PendingIntent = PendingIntent.getActivity(appContext, courseId, intent, pendingIntentFlags)

        val inboxStyle = NotificationCompat.InboxStyle()
        val title = courseName
        val summaryText = "${fileRecords.size} nuovi file"
        
        inboxStyle.setBigContentTitle(title)
        
        fileRecords.take(5).forEach { record ->
            inboxStyle.addLine("📄 ${record.fileName}")
        }
        
        if (fileRecords.size > 5) {
            inboxStyle.setSummaryText(appContext.getString(R.string.sync_more_files, fileRecords.size - 5))
        }

        val builder = NotificationCompat.Builder(appContext, channelId)
            .setSmallIcon(android.R.drawable.sym_def_app_icon)
            .setContentTitle(title)
            .setContentText(summaryText)
            .setStyle(inboxStyle)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            // group key must match the summary notification
            .setGroup("WE_BEEP_SYNC_GROUP")
            .setAutoCancel(true)

        // using courseid ensures notifications don't overwrite each other across different courses
        notificationManager.notify(courseId, builder.build())
        Log.d("CourseSyncWorker", "📢 Notifica inviata per il corso $courseId")
    }
}
