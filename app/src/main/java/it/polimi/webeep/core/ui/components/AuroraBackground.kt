package it.polimi.webeep.core.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * A beautiful, animated aurora/glow background that adapts to light/dark mode.
 * In dark mode: deep dark base with neon cyan and electric blue soft glows.
 * In light mode: ice white base with soft pastel sky blue and indigo glows.
 */
@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier,
    isDark: Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "aurora_anim")
    
    // main aurora glow paths
    val glowX1 by infiniteTransition.animateFloat(
        initialValue = -500f,
        targetValue = 1500f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aurora_x1"
    )
    val glowY1 by infiniteTransition.animateFloat(
        initialValue = -500f,
        targetValue = 2000f,
        animationSpec = infiniteRepeatable(
            animation = tween(22000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aurora_y1"
    )

    // secondary aurora glow paths
    val glowX2 by infiniteTransition.animateFloat(
        initialValue = 1800f,
        targetValue = -200f,
        animationSpec = infiniteRepeatable(
            animation = tween(18000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aurora_x2"
    )
    val glowY2 by infiniteTransition.animateFloat(
        initialValue = 800f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aurora_y2"
    )

    // tertiary aurora glow paths
    val glowX3 by infiniteTransition.animateFloat(
        initialValue = 500f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aurora_x3"
    )
    val glowY3 by infiniteTransition.animateFloat(
        initialValue = 2200f,
        targetValue = -300f,
        animationSpec = infiniteRepeatable(
            animation = tween(25000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aurora_y3"
    )

    // breathing effect for radius
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aurora_pulse"
    )

    // palette based on mode
    val backgroundColor = if (isDark) Color(0xFF020813) else Color(0xFFF4F7FA)
    
    val primaryGlowColor = if (isDark) {
        Color(0xFF0066CC).copy(alpha = 0.45f)
    } else {
        Color(0xFFA5C8FF).copy(alpha = 0.6f) // soft pastel blue
    }
    
    val primaryGlowFade = if (isDark) {
        Color(0xFF0A3366).copy(alpha = 0.2f)
    } else {
        Color(0xFF0091FF).copy(alpha = 0.1f)
    }

    val secondaryGlowColor = if (isDark) {
        Color(0xFF00C2FF).copy(alpha = 0.3f) // neon cyan
    } else {
        Color(0xFF81B5FF).copy(alpha = 0.45f) // indigo/blue pastel
    }

    val tertiaryGlowColor = if (isDark) {
        Color(0xFF8A2BE2).copy(alpha = 0.2f) // deep purple for dark mode
    } else {
        Color(0xFFFFB6C1).copy(alpha = 0.35f) // soft pastel pink for light mode
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // base solid background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
        )

        // main aurora glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryGlowColor,
                            primaryGlowFade,
                            Color.Transparent
                        ),
                        center = Offset(glowX1, glowY1),
                        radius = 1800f * pulseScale
                    )
                )
        )

        // secondary counter-phase aurora glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            secondaryGlowColor,
                            Color.Transparent
                        ),
                        center = Offset(glowX2, glowY2),
                        radius = 1400f * (2f - pulseScale) // inverse pulse
                    )
                )
        )

        // tertiary dynamic aurora glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            tertiaryGlowColor,
                            Color.Transparent
                        ),
                        center = Offset(glowX3, glowY3),
                        radius = 1600f * pulseScale
                    )
                )
        )

        // the actual screen content
        content()
    }
}
