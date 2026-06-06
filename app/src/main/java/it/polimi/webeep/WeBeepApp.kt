package it.polimi.webeep

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import it.polimi.webeep.core.datastore.TokenManager
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
    initialDeepLinkToken: String?,
    onNavControllerReady: (androidx.navigation.NavHostController) -> Unit = {}
) {
    var startDestination by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(initialDeepLinkToken) {
        // determine the initial screen based on whether we have a deep link or an active session
        startDestination = when {
            initialDeepLinkToken != null -> Screen.Login.createRoute(initialDeepLinkToken)
            tokenManager.isLoggedIn.first() -> Screen.Dashboard.route
            else -> Screen.Login.route
        }
    }

    WeBeepTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            when (val destination = startDestination) {
                null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                else -> {
                    val navController = rememberNavController()
                    LaunchedEffect(navController) {
                        onNavControllerReady(navController)
                    }
                    NavGraph(
                        startDestination = destination,
                        navController = navController
                    )
                }
            }
        }
    }
}
