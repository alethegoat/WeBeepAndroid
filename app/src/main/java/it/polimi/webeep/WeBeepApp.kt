package it.polimi.webeep

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import it.polimi.webeep.core.datastore.SettingsManager
import it.polimi.webeep.core.datastore.ThemePreference
import it.polimi.webeep.core.datastore.TokenProvider
import it.polimi.webeep.core.session.SessionExpiryHandler
import it.polimi.webeep.core.ui.components.AuroraBackground
import it.polimi.webeep.core.ui.theme.WeBeepTheme
import it.polimi.webeep.navigation.NavGraph
import it.polimi.webeep.navigation.Screen
import it.polimi.webeep.navigation.navigateToLogin
import kotlinx.coroutines.flow.first

@Composable
fun WeBeepApp(
    tokenProvider: TokenProvider,
    settingsManager: SettingsManager,
    sessionExpiryHandler: SessionExpiryHandler,
    startAtLogin: Boolean,
    onNavControllerReady: (NavHostController) -> Unit = {}
) {
    val themePreference by settingsManager.themePreference.collectAsStateWithLifecycle(
        initialValue = ThemePreference.SYSTEM_DEFAULT
    )
    val dynamicColor by settingsManager.dynamicColorPreference.collectAsStateWithLifecycle(
        initialValue = false
    )

    var startDestination by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        // Keystore-backed prefs are opened on IO here, not on the main thread.
        tokenProvider.ensureLoaded()
        startDestination = when {
            startAtLogin -> Screen.Login.route
            tokenProvider.isLoggedIn.first() -> Screen.Dashboard.route
            else -> Screen.Login.route
        }
        sessionExpiryHandler.wipeStaleDataIfLoggedOut()
    }

    val isDarkTheme = when (themePreference) {
        ThemePreference.SYSTEM_DEFAULT -> isSystemInDarkTheme()
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
    }

    WeBeepTheme(
        darkTheme = isDarkTheme,
        dynamicColor = dynamicColor
    ) {
        AuroraBackground {
            when (val destination = startDestination) {
                null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                else -> {
                    val navController = rememberNavController()
                    LaunchedEffect(navController) {
                        onNavControllerReady(navController)
                    }
                    LaunchedEffect(navController, sessionExpiryHandler) {
                        sessionExpiryHandler.sessionLost.collect {
                            navController.navigateToLogin()
                        }
                    }
                    NavGraph(
                        startDestination = destination,
                        navController = navController,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
