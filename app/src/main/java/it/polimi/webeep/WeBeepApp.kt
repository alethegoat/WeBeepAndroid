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
import androidx.navigation.compose.rememberNavController
import it.polimi.webeep.core.datastore.SettingsManager
import it.polimi.webeep.core.datastore.ThemePreference
import it.polimi.webeep.core.datastore.TokenManager
import it.polimi.webeep.core.ui.components.AuroraBackground
import it.polimi.webeep.core.ui.theme.WeBeepTheme
import it.polimi.webeep.navigation.NavGraph
import it.polimi.webeep.navigation.Screen
import kotlinx.coroutines.flow.first

/**
 * main entry point composable for the webeep app.
 */
@Composable
fun WeBeepApp(
    tokenManager: TokenManager,
    settingsManager: SettingsManager,
    initialDeepLinkToken: String?,
    onNavControllerReady: (androidx.navigation.NavHostController) -> Unit = {}
) {
    val themePreference by settingsManager.themePreference.collectAsStateWithLifecycle(
        initialValue = ThemePreference.SYSTEM_DEFAULT
    )
    val dynamicColor by settingsManager.dynamicColorPreference.collectAsStateWithLifecycle(
        initialValue = false
    )

    var startDestination by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(initialDeepLinkToken) {
        // determine the initial screen based on whether we have a deep link or an active session
        startDestination = when {
            initialDeepLinkToken != null -> Screen.Login.createRoute(initialDeepLinkToken)
            tokenManager.isLoggedIn.first() -> Screen.Dashboard.route
            else -> Screen.Login.route
        }
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
