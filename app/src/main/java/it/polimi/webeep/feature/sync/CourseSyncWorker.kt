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

    data class NewFileRecord(val courseName: String, val fileName: String)

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
                                        // unique key: combine url and filename to avoid timemodified collisions
                                        val uniqueKey = "${content.fileUrl}_${content.filename}"
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

                                    mod.contents?.forEach { content ->
                                        // ignore empty directories from moodle api (they cause fake files and false notifications)
                                        if (content.type == "dir") return@forEach

                                        courseFilesCount++
                                        val filename = MoodleText.parse(content.filename ?: "Senza nome")
                                        val fileUrl = content.fileurl ?: ""
                                        val filepath = content.filepath
                                        
                                        val uniqueKey = "${fileUrl}_${filename}"

                                        // correct diffing: verify url+name.
                                        // adding isnotempty() prevents notification spam on absolute first launch.
                                        if (!oldFilesKeys.contains(uniqueKey) && oldFilesKeys.isNotEmpty()) {
                                            Log.d("CourseSyncWorker", "✨ [${course.shortname}] Trovato nuovo file: $filename")
                                            currentNewFiles.add(NewFileRecord(course.fullname, filename))
                                        }

                                        contentEntities.add(
                                            ModuleContentEntity(
                                                moduleId = mod.id,
                                                filename = filename,
                                                filepath = filepath,
                                                fileUrl = fileUrl,
                                                fileSize = content.filesize,
                                                mimeType = content.mimetype,
                                                timeModified = content.timemodified
                                            )
                                        )
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
                            Log.e("CourseSyncWorker", "❌ [${course.shortname}] Errore durante il sync: ${e.message}")
                            hasErrors = true
                        }
                    }
                }
                jobs.awaitAll()
            }
            
            Log.d("CourseSyncWorker", "✅ Sync completato. File nuovi totali confermati: $totalNewFiles")
            
            if (totalNewFiles > 0) {
                Log.d("CourseSyncWorker", "🔔 Invio notifica per $totalNewFiles file totali.")
                sendNotification(totalNewFiles, newFileRecords)
            }

            return if (hasErrors) Result.retry() else Result.success()
        } catch (e: Exception) {
            Log.e("CourseSyncWorker", "💥 Errore fatale nel Worker: ${e.message}")
            e.printStackTrace()
            return Result.retry()
        }
    }

    private fun sendNotification(newFilesCount: Int, fileRecords: List<NewFileRecord>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d("CourseSyncWorker", "🔕 Permesso notifiche non concesso.")
            return
        }

        val channelId = "webeep_sync_channel"
        val notificationManager = NotificationManagerCompat.from(appContext)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Nuovi Documenti WeBeep",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifiche per i nuovi materiali caricati nei corsi"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        
        val pendingIntent: PendingIntent = PendingIntent.getActivity(appContext, 0, intent, pendingIntentFlags)

        val inboxStyle = NotificationCompat.InboxStyle()
        val title = "Nuovi materiali su WeBeep!"
        val summaryText = "Sono stati caricati $newFilesCount nuovi file."
        
        inboxStyle.setBigContentTitle(title)
        
        fileRecords.take(5).forEach { record ->
            inboxStyle.addLine("📄 ${record.fileName} - ${record.courseName}")
        }
        
        if (fileRecords.size > 5) {
            inboxStyle.setSummaryText("+ altri ${fileRecords.size - 5} file")
        }

        val builder = NotificationCompat.Builder(appContext, channelId)
            .setSmallIcon(android.R.drawable.sym_def_app_icon)
            .setContentTitle(title)
            .setContentText(summaryText)
            .setStyle(inboxStyle)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        // static id (e.g. 1001) to avoid overwriting with separate notifications,
        // but smart updating of the grouped inboxstyle.
        val notificationId = 1001
        notificationManager.notify(notificationId, builder.build())
        Log.d("CourseSyncWorker", "📢 Notifica raggruppata aggiornata/mostrata con ID $notificationId")
    }
}
