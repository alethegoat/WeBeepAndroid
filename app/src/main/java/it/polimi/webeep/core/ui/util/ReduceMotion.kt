package it.polimi.webeep.core.ui.util

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

/**
 * True when the user asked the system to remove / shorten animations.
 * Read [Settings.Global.ANIMATOR_DURATION_SCALE] and
 * [Settings.Global.TRANSITION_ANIMATION_SCALE]; 0 means "don't animate".
 */
val LocalReduceMotion = staticCompositionLocalOf { false }

/** Reads the system animation scales once per composition context; unreadable settings count as "animate". */
@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        fun scale(name: String): Float = try {
            Settings.Global.getFloat(context.contentResolver, name, 1f)
        } catch (_: Throwable) {
            1f
        }
        scale(Settings.Global.ANIMATOR_DURATION_SCALE) == 0f ||
            scale(Settings.Global.TRANSITION_ANIMATION_SCALE) == 0f
    }
}
