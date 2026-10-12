package it.polimi.webeep.util

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.test.platform.app.InstrumentationRegistry
import it.polimi.webeep.core.ui.theme.WeBeepTheme
import java.time.Duration
import org.robolectric.shadows.ShadowSystemClock

/** Resolves a string resource the way the screen under test does, in whatever locale Robolectric runs. */
fun string(@StringRes id: Int, vararg args: Any): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

/** Hosts [content] in the app theme, fixed to light so the colors never depend on the test device. */
fun ComposeContentTestRule.setAppContent(content: @Composable () -> Unit) {
    // bouncyClickable ignores taps within 250 ms of the clock's zero, which Robolectric starts at.
    ShadowSystemClock.advanceBy(Duration.ofSeconds(1))
    setContent {
        WeBeepTheme(darkTheme = false) { content() }
    }
}
