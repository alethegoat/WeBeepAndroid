package it.polimi.webeep

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.AndroidEntryPoint
import it.polimi.webeep.core.datastore.SettingsManager
import it.polimi.webeep.core.datastore.TokenManager
import it.polimi.webeep.feature.sync.CourseSyncWorker
import it.polimi.webeep.navigation.Screen
import java.util.concurrent.TimeUnit
import javax.inject.Inject

import android.view.WindowManager

/**
 * main activity of the webeep application.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var tokenManager: TokenManager

    @Inject
    lateinit var settingsManager: SettingsManager

    private var navController: NavHostController? = null
    private var pendingToken: String? = null
    private var pendingCourseId: Int? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // the background sync should run regardless of whether notifications are permitted
        setupPeriodicSync()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()

        // check if the activity was launched via a sso deep link or a new file notification
        checkSsoDeepLink(intent)
        checkCourseIntent(intent)

        setContent {
            WeBeepApp(
                tokenManager = tokenManager,
                settingsManager = settingsManager,
                initialDeepLinkToken = pendingToken,
                onNavControllerReady = { controller ->
                    navController = controller
                    
                    // consume pending token if any
                    pendingToken = null
                    
                    // consume pending course id if the user clicked a notification
                    // we immediately redirect them to the relevant course details.
                    pendingCourseId?.let { courseId ->
                        controller.navigate(Screen.CourseDetail.createRoute(courseId))
                        pendingCourseId = null
                    }
                }
            )
        }

        checkAndRequestNotificationPermission()
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                setupPeriodicSync()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            setupPeriodicSync()
        }
    }

    private fun setupPeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true) // battery optimization: do not sync if battery is low
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<CourseSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "CourseSyncWorker",
            ExistingPeriodicWorkPolicy.UPDATE, // update ensures the change takes effect even for those who already had the rule active
            syncRequest
        )
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // re-check for new intents if the app is already in the foreground or background
        checkSsoDeepLink(intent)
        checkCourseIntent(intent)
        
        // handle sso login callback
        pendingToken?.let { token ->
            navController?.navigate(Screen.Login.createRoute(token)) {
                popUpTo(navController!!.graph.startDestinationId) { inclusive = true }
            }
            pendingToken = null
        }
        
        // handle notification click to jump into course details
        pendingCourseId?.let { courseId ->
            navController?.navigate(Screen.CourseDetail.createRoute(courseId))
            pendingCourseId = null
        }
    }

    /**
     * checks if the intent has a token for sso login and saves it
     */
    private fun checkSsoDeepLink(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme !in SSO_CALLBACK_SCHEMES) return

        extractToken(data)?.let { token ->
            pendingToken = token
        }
    }

    /**
     * checks if the intent comes from a coursesyncworker notification
     * and prepares the app to navigate to the specific course.
     */
    private fun checkCourseIntent(intent: Intent?) {
        val courseId = intent?.getIntExtra("EXTRA_COURSE_ID", -1) ?: -1
        if (courseId != -1) {
            pendingCourseId = courseId
        }
    }

    private fun extractToken(data: Uri): String? {
        data.getQueryParameter("token")?.takeIf { it.isNotBlank() }?.let { return it }

        val encodedSchemeSpecificPart = data.encodedSchemeSpecificPart.orEmpty()
        val encodedToken = when {
            encodedSchemeSpecificPart.startsWith("//token=") ->
                encodedSchemeSpecificPart.removePrefix("//token=").substringBefore("&")
            encodedSchemeSpecificPart.startsWith("token=") ->
                encodedSchemeSpecificPart.removePrefix("token=").substringBefore("&")
            else ->
                data.toString().substringAfter("token=", missingDelimiterValue = "").substringBefore("&")
        }

        return Uri.decode(encodedToken).takeIf { it.isNotBlank() }
    }

    private companion object {
        val SSO_CALLBACK_SCHEMES = setOf("webeepandroid", "moodlemobile")
    }
}
