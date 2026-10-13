package it.polimi.webeep.core.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The app's indeterminate spinner: thin stroke, so it reads as calm at any size. */
@Composable
fun LoadingIndicator(size: Dp, modifier: Modifier = Modifier, color: Color = ProgressIndicatorDefaults.circularColor) {
    CircularProgressIndicator(
        color = color,
        strokeWidth = 2.5.dp,
        modifier = modifier.size(size)
    )
}
