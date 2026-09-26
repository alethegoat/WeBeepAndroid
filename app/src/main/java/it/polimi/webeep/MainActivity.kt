package it.polimi.webeep

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import dagger.hilt.android.AndroidEntryPoint
import it.polimi.webeep.core.datastore.SettingsManager
import it.polimi.webeep.core.datastore.TokenProvider
import it.polimi.webeep.core.session.SessionExpiryHandler
import it.polimi.webeep.feature.auth.domain.SsoCallbackParser
import it.polimi.webeep.feature.auth.domain.SsoCallbackRelay
import it.polimi.webeep.feature.sync.CourseSyncWorker
import it.polimi.webeep.feature.sync.SyncScheduler
import it.polimi.webeep.navigation.Screen
import it.polimi.webeep.navigation.navigateToLogin
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var tokenProvider: TokenProvider

    @Inject
    lateinit var settingsManager: SettingsManager

    @Inject
    lateinit var syncScheduler: SyncScheduler

    @Inject
    lateinit var ssoCallbackRelay: SsoCallbackRelay

    @Inject
    lateinit var sessionExpiryHandler: SessionExpiryHandler

    private var navController: NavHostController? = null
    private var pendingCourseId: Int? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _: Boolean ->
        setupPeriodicSync()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must run before super.onCreate: swaps Theme.WeBeep.Starting for Theme.WeBeep.
        installSplashScreen()
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        // Intents are replayed on recreation (rotation, process death): consume them only once.
        val isFreshStart = savedInstanceState == null
        if (isFreshStart) {
            consumeSsoDeepLink(intent)
            checkCourseIntent(intent)
        }
        val startAtLogin = ssoCallbackRelay.hasPending

        setContent {
            WeBeepApp(
                tokenProvider = tokenProvider,
                settingsManager = settingsManager,
                sessionExpiryHandler = sessionExpiryHandler,
                startAtLogin = startAtLogin,
                onNavControllerReady = { controller ->
                    navController = controller
                    if (ssoCallbackRelay.hasPending) {
                        controller.navigateToLogin()
                    }
                    openPendingCourse(controller)
                }
            )
        }

        if (isFreshStart) {
            checkAndRequestNotificationPermission()
        }
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
        val hasSsoCallback = consumeSsoDeepLink(intent)
        checkCourseIntent(intent)
        setIntent(intent)

        // Without a NavController yet, onNavControllerReady routes to login.
        val controller = navController ?: return
        if (hasSsoCallback) {
            controller.navigateToLogin()
        }
        openPendingCourse(controller)
    }

    private fun openPendingCourse(controller: NavHostController) {
        val courseId = pendingCourseId ?: return
        pendingCourseId = null
        if (tokenProvider.getTokenSync() != null) {
            controller.navigate(Screen.CourseDetail.createRoute(courseId))
        }
    }

    /**
     * Hands the SSO blob to [SsoCallbackRelay] and strips it from the intent, so it is
     * never kept by the Activity, the task record or saved state.
     */
    private fun consumeSsoDeepLink(intent: Intent): Boolean {
        val data = intent.data ?: return false
        if (data.scheme !in SSO_CALLBACK_SCHEMES) return false
        val token = SsoCallbackParser.extractToken(data)
        intent.data = null
        token?.let(ssoCallbackRelay::offer)
        return token != null
    }

    private fun checkCourseIntent(intent: Intent) {
        val courseId = intent.getIntExtra(CourseSyncWorker.EXTRA_COURSE_ID, -1)
        if (courseId > 0) {
            pendingCourseId = courseId
            intent.removeExtra(CourseSyncWorker.EXTRA_COURSE_ID)
        }
    }

    private companion object {
        val SSO_CALLBACK_SCHEMES = setOf("webeepandroid", "moodlemobile")
    }
}
