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
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import it.polimi.webeep.core.datastore.SettingsManager
import it.polimi.webeep.core.datastore.TokenManager
import it.polimi.webeep.feature.sync.SyncScheduler
import it.polimi.webeep.navigation.Screen
import kotlinx.coroutines.launch
import javax.inject.Inject

import android.view.WindowManager

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var tokenManager: TokenManager

    @Inject
    lateinit var settingsManager: SettingsManager

    @Inject
    lateinit var syncScheduler: SyncScheduler

    private var navController: NavHostController? = null
    private var pendingToken: String? = null
    private var pendingCourseId: Int? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        setupPeriodicSync()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()

        checkSsoDeepLink(intent)
        checkCourseIntent(intent)

        setContent {
            WeBeepApp(
                tokenManager = tokenManager,
                settingsManager = settingsManager,
                initialDeepLinkToken = pendingToken,
                onNavControllerReady = { controller ->
                    navController = controller
                    pendingToken = null

                    pendingCourseId?.let { courseId ->
                        if (tokenManager.getTokenSync() != null) {
                            controller.navigate(Screen.CourseDetail.createRoute(courseId))
                        }
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
        lifecycleScope.launch {
            syncScheduler.applyCurrentSettings(replaceExisting = false)
            syncScheduler.enqueueImmediate()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        checkSsoDeepLink(intent)
        checkCourseIntent(intent)

        pendingToken?.let { token ->
            navController?.navigate(Screen.Login.createRoute(token)) {
                popUpTo(navController!!.graph.startDestinationId) { inclusive = true }
            }
            pendingToken = null
        }
        
        pendingCourseId?.let { courseId ->
            if (tokenManager.getTokenSync() != null) {
                navController?.navigate(Screen.CourseDetail.createRoute(courseId))
            }
            pendingCourseId = null
        }
    }

    private fun checkSsoDeepLink(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme !in SSO_CALLBACK_SCHEMES) return

        it.polimi.webeep.feature.auth.domain.SsoCallbackParser.extractToken(data)?.let { token ->
            pendingToken = token
        }
    }

    private fun checkCourseIntent(intent: Intent?) {
        val courseId = intent?.getIntExtra(
            it.polimi.webeep.feature.sync.CourseSyncWorker.EXTRA_COURSE_ID, -1
        ) ?: -1
        if (courseId > 0) {
            pendingCourseId = courseId
        }
    }

    private companion object {
        val SSO_CALLBACK_SCHEMES = setOf("webeepandroid", "moodlemobile")
    }
}
