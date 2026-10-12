package it.polimi.webeep.feature.settings.presentation.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import it.polimi.webeep.R
import it.polimi.webeep.core.ui.components.BackNavigationIcon

/** Large title that collapses into the app bar while the settings scroll. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTopBar(scrollBehavior: TopAppBarScrollBehavior, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    LargeTopAppBar(
        title = { Text(text = stringResource(R.string.settings)) },
        modifier = modifier,
        navigationIcon = { BackNavigationIcon(onBack = onBackClick) },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
            titleContentColor = MaterialTheme.colorScheme.onBackground
        ),
        scrollBehavior = scrollBehavior
    )
}
