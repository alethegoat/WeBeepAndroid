package it.polimi.webeep.core.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * color palette inspired by politecnico di milano, optimized for material 3.
 */

// ── Primary (PoliMi Heritage Blue / WeBeep Teal) ──────────────
val PoliMiNavy = Color(0xFF102C53) // Heritage Blue
val PoliMiNavyLight = Color(0xFF1F437A)
val PoliMiNavyDark = Color(0xFF071529)

// ── Secondary (PoliMi Alpha Blue / Cyan) ───────────────────────
val PoliMiBrightBlue = Color(0xFF0BA4FF) // Alpha Blue
val PoliMiBrightBlueLight = Color(0xFF5CC2FF)
val PoliMiBrightBlueDark = Color(0xFF007ECC)

// ── Tertiary / Accent ────────────────────────────────────────────────
val PoliMiRed = Color(0xFFE63946)
val PoliMiRedLight = Color(0xFFFF6B6B)
val PoliMiRedDark = Color(0xFFB71C2A)

// ── Error ────────────────────────────────────────────────────────────
val ErrorRed = Color(0xFFBA1A1A)
val ErrorRedDark = Color(0xFFFFB4AB)
val OnErrorLight = Color(0xFFFFFFFF)
val OnErrorDark = Color(0xFF690005)
val ErrorContainerLight = Color(0xFFFFDAD6)
val ErrorContainerDark = Color(0xFF93000A)
val OnErrorContainerLight = Color(0xFF410002)
val OnErrorContainerDark = Color(0xFFFFDAD6)

// ── Light Theme ──────────────────────────────────────────────────────
val LightBackground = Color(0xFFFAFBFF)
val LightSurface = Color(0xFFFAFBFF)
val LightSurfaceVariant = Color(0xFFDFE2EB)
val LightOnBackground = Color(0xFF1A1C1E)
val LightOnSurface = Color(0xFF1A1C1E)
val LightOnSurfaceVariant = Color(0xFF43474E)
val LightOutline = Color(0xFF73777F)
val LightOutlineVariant = Color(0xFFC3C6CF)
val LightInverseSurface = Color(0xFF2F3033)
val LightInverseOnSurface = Color(0xFFF1F0F4)
val LightInversePrimary = Color(0xFFA5C8FF)
val LightSurfaceTint = PoliMiNavy

// ── Dark Theme (OLED Optimized) ──────────────────────────────────────
val DarkBackground = Color(0xFF000000) // pure black for oled
val DarkSurface = Color(0xFF000000)    // pure black for oled
val DarkSurfaceVariant = Color(0xFF1C1D20) // slightly elevated
val DarkOnBackground = Color(0xFFE3E2E6)
val DarkOnSurface = Color(0xFFE3E2E6)
val DarkOnSurfaceVariant = Color(0xFFC3C6CF)
val DarkOutline = Color(0xFF8D9199)
val DarkOutlineVariant = Color(0xFF43474E)
val DarkInverseSurface = Color(0xFFE3E2E6)
val DarkInverseOnSurface = Color(0xFF2F3033)
val DarkInversePrimary = PoliMiNavy
val DarkSurfaceTint = PoliMiBrightBlueLight

// ── Colori on-* (Light) ─────────────────────────────────────────────
val OnPrimaryLight = Color(0xFFFFFFFF)
val OnPrimaryContainerLight = Color(0xFF001B3D)
val PrimaryContainerLight = Color(0xFFD6E3FF)
val OnSecondaryLight = Color(0xFFFFFFFF)
val OnSecondaryContainerLight = Color(0xFF001F2A)
val SecondaryContainerLight = Color(0xFFBFE9FF)
val OnTertiaryLight = Color(0xFFFFFFFF)
val OnTertiaryContainerLight = Color(0xFF410002)
val TertiaryContainerLight = Color(0xFFFFDAD6)

// ── Colori on-* (Dark) ──────────────────────────────────────────────
val OnPrimaryDark = Color(0xFF003062)
val OnPrimaryContainerDark = Color(0xFFD6E3FF)
val PrimaryContainerDark = Color(0xFF00468A)
val OnSecondaryDark = Color(0xFF003549)
val OnSecondaryContainerDark = Color(0xFFBFE9FF)
val SecondaryContainerDark = Color(0xFF004C68)
val OnTertiaryDark = Color(0xFF690005)
val OnTertiaryContainerDark = Color(0xFFFFDAD6)
val TertiaryContainerDark = Color(0xFF93000A)

// ── File Type Semantic Colors ────────────────────────────────────────
val ColorPdf = Color(0xFFE53935)
val ColorWord = Color(0xFF1E88E5)
val ColorExcel = Color(0xFF43A047)
val ColorPowerPoint = Color(0xFFFB8C00)
val ColorZip = Color(0xFF8E24AA)
val ColorFolder = Color(0xFFFFB300)
val ColorLink = Color(0xFF039BE5)
val ColorGenericFile = Color(0xFF757575)

// ── Gradients & Glassmorphism ────────────────────────────────────────
val GradientDarkStart = Color(0xFF102C53) // PoliMi Heritage Blue
val GradientDarkEnd = Color(0xFF0BA4FF)   // PoliMi Alpha Blue
val GradientOverlayStart = Color(0xD9000000) // 85% opacity black
val GradientOverlayEnd = Color(0x26000000)   // 15% opacity black

val GlassWhite = Color(0x2AFFFFFF) // highly translucent white for pills
val GlassDark = Color(0x40000000)
val GlassSurfaceLight = Color(0xB3FFFFFF) // 70% opacity
val GlassSurfaceDark = Color(0xB3161B22)

// ── Shimmer ──────────────────────────────────────────────────────────
val ShimmerLightBase = Color(0xFFE0E0E0)
val ShimmerLightHighlight = Color(0xFFF5F5F5)
val ShimmerDarkBase = Color(0xFF2A2A2A)
val ShimmerDarkHighlight = Color(0xFF3D3D3D)
