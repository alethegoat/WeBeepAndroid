package it.polimi.webeep

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import it.polimi.webeep.core.datastore.TokenProvider
import it.polimi.webeep.core.session.SessionExpiryHandler
import it.polimi.webeep.core.ui.theme.InterFamily
import it.polimi.webeep.core.ui.theme.OutfitFamily
import it.polimi.webeep.core.ui.theme.WeBeepTheme
import it.polimi.webeep.feature.settings.domain.SettingsRepository
import it.polimi.webeep.feature.settings.domain.model.ThemePreference
import it.polimi.webeep.navigation.NavGraph
import it.polimi.webeep.navigation.Screen
import it.polimi.webeep.navigation.navigateToLogin
import kotlinx.coroutines.flow.first

@Composable
fun WeBeepApp(
    tokenProvider: TokenProvider,
    settingsRepository: SettingsRepository,
    sessionExpiryHandler: SessionExpiryHandler,
    startAtLogin: Boolean,
    onNavControllerReady: (NavHostController) -> Unit = {}
) {
    val themePreference by settingsRepository.themePreference.collectAsStateWithLifecycle(
        initialValue = ThemePreference.SYSTEM_DEFAULT
    )
    val dynamicColor by settingsRepository.dynamicColorPreference.collectAsStateWithLifecycle(
        initialValue = false
    )

    // Saved, and the token store outlives the Activity: rotating or switching the
    // language recreates the screen in place instead of flashing the spinner. Only a
    // cold start (or process death) waits for the store to open.
    var startDestination by rememberSaveable { mutableStateOf<String?>(null) }
    var storeReady by remember { mutableStateOf(tokenProvider.isLoaded) }

    LaunchedEffect(Unit) {
        // Keystore-backed prefs are opened on IO here, not on the main thread.
        tokenProvider.ensureLoaded()
        storeReady = true
        if (startDestination == null) {
            startDestination = when {
                startAtLogin -> Screen.Login.route
                tokenProvider.isLoggedIn.first() -> Screen.Dashboard.route
                else -> Screen.Login.route
            }
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
        val navController = rememberNavController()
        val fontResolver = LocalFontFamilyResolver.current
        LaunchedEffect(fontResolver) {
            // Warms every typeface instance up front, so the first screen that uses a
            // new weight (often mid-transition) does not stall on font loading.
            fontResolver.preload(OutfitFamily)
            fontResolver.preload(InterFamily)
        }
        // Each screen paints its own background (the login one its animated aurora).
        when (val destination = startDestination?.takeIf { storeReady }) {
            null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            else -> {
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
