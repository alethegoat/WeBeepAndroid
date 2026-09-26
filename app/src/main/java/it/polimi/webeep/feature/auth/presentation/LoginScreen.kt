package it.polimi.webeep.feature.auth.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.polimi.webeep.R
import it.polimi.webeep.core.ui.components.WeBeepSnackbarHost
import it.polimi.webeep.core.ui.components.showErrorSnackbar
import it.polimi.webeep.core.ui.theme.WeBeepMotion
import it.polimi.webeep.core.ui.util.LocalReduceMotion
import it.polimi.webeep.core.ui.util.bouncyClickable
import kotlinx.coroutines.delay

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val retryLabel = stringResource(R.string.retry)

    val reduceMotion = LocalReduceMotion.current
    var logoVisible by remember { mutableStateOf(reduceMotion) }
    var cardVisible by remember { mutableStateOf(reduceMotion) }

    LaunchedEffect(reduceMotion) {
        if (reduceMotion) {
            logoVisible = true
            cardVisible = true
        } else {
            logoVisible = true
            delay(120)
            cardVisible = true
        }
    }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is LoginUiState.Success -> onLoginSuccess()
            is LoginUiState.Error -> {
                showErrorSnackbar(
                    snackbarHostState = snackbarHostState,
                    message = state.message,
                    actionLabel = retryLabel
                )
                viewModel.clearError()
            }
            else -> Unit
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        snackbarHost = { WeBeepSnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 28.dp)
            ) {
                AnimatedVisibility(
                    visible = logoVisible,
                    enter = fadeIn(WeBeepMotion.enter(WeBeepMotion.LongMs)) +
                        slideInVertically(WeBeepMotion.enter(WeBeepMotion.LongMs)) { -36 }
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(
                            painter = painterResource(R.drawable.ic_webeep_mark),
                            contentDescription = stringResource(R.string.app_name),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(96.dp)
                                .shadow(18.dp, RoundedCornerShape(28.dp), clip = false)
                                .clip(RoundedCornerShape(28.dp))
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        )
                        Text(
                            text = stringResource(R.string.politecnico_milano),
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                AnimatedVisibility(
                    visible = cardVisible,
                    enter = fadeIn(WeBeepMotion.enter(WeBeepMotion.LongMs)) +
                        slideInVertically(WeBeepMotion.enter(WeBeepMotion.LongMs)) { 40 }
                ) {
                    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
                    val cardShape = RoundedCornerShape(28.dp)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surface.copy(
                                    alpha = if (isDark) 0.28f else 0.72f
                                ),
                                shape = cardShape
                            )
                            .border(
                                width = 1.dp,
                                brush = Brush.linearGradient(
                                    listOf(
                                        Color.White.copy(alpha = if (isDark) 0.28f else 0.78f),
                                        Color.White.copy(alpha = 0.06f)
                                    )
                                ),
                                shape = cardShape
                            )
                            .padding(horizontal = 28.dp, vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.login_subtitle),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.78f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 28.dp)
                        )

                        val isLoading = uiState is LoginUiState.Loading
                        val buttonShape = RoundedCornerShape(18.dp)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .shadow(
                                    elevation = if (isLoading) 0.dp else 10.dp,
                                    shape = buttonShape,
                                    clip = false,
                                    ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                )
                                .clip(buttonShape)
                                .background(
                                    brush = Brush.horizontalGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.secondary
                                        )
                                    )
                                )
                                .bouncyClickable(enabled = !isLoading) {
                                    SsoBrowser.open(context, viewModel.generateSsoUrl())
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Crossfade(
                                targetState = isLoading,
                                animationSpec = WeBeepMotion.smooth(WeBeepMotion.ShortMs),
                                label = "login_cta"
                            ) { loading ->
                                if (loading) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.5.dp,
                                        modifier = Modifier.size(26.dp)
                                    )
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Login,
                                            contentDescription = null,
                                            modifier = Modifier.padding(end = 8.dp),
                                            tint = MaterialTheme.colorScheme.onPrimary
                                        )
                                        Text(
                                            text = stringResource(R.string.login_sso_button),
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        TextButton(
                            onClick = { SsoBrowser.openInExternalBrowser(context, viewModel.generateSsoUrl()) },
                            enabled = !isLoading,
                            modifier = Modifier.padding(top = 12.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.login_sso_external_browser),
                                style = MaterialTheme.typography.labelLarge,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
}
