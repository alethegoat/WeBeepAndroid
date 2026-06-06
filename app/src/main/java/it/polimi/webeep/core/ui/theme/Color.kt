package it.polimi.webeep.core.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * color palette inspired by politecnico di milano, optimized for material 3.
 */

// ── primary (polimi heritage blue / webeep teal) ──────────────
val PoliMiNavy = Color(0xFF0A3366) // vibrant heritage blue
val PoliMiNavyLight = Color(0xFF164B8E)
val PoliMiNavyDark = Color(0xFF041C3D)

// ── secondary (polimi alpha blue / cyan) ───────────────────────
val PoliMiBrightBlue = Color(0xFF0091FF) // vibrant alpha blue
val PoliMiBrightBlueLight = Color(0xFF00C2FF) // electric cyan for dark mode
val PoliMiBrightBlueDark = Color(0xFF0066CC)

// ── tertiary / accent ────────────────────────────────────────────────
val PoliMiRed = Color(0xFFE63946)
val PoliMiRedLight = Color(0xFFFF6B6B)
val PoliMiRedDark = Color(0xFFB71C2A)

// ── error ────────────────────────────────────────────────────────────
val ErrorRed = Color(0xFFE04040) // coral red
val ErrorRedDark = Color(0xFFFF6B6B) // soft coral for dark
val OnErrorLight = Color(0xFFFFFFFF)
val OnErrorDark = Color(0xFF3B0000)
val ErrorContainerLight = Color(0xFFFFE0E0)
val ErrorContainerDark = Color(0xFF5C0000)
val OnErrorContainerLight = Color(0xFF4C0000)
val OnErrorContainerDark = Color(0xFFFFE0E0)

// ── light theme ──────────────────────────────────────────────────────
val LightBackground = Color(0xFFF7F9FC) // ice grey
val LightSurface = Color(0xFFFFFFFF) // pure white
val LightSurfaceVariant = Color(0xFFEDF1F5) // clean grey-blue
val LightOnBackground = Color(0xFF11141A)
val LightOnSurface = Color(0xFF11141A)
val LightOnSurfaceVariant = Color(0xFF4A5568)
val LightOutline = Color(0xFFA0AEC0)
val LightOutlineVariant = Color(0xFFCBD5E0)
val LightInverseSurface = Color(0xFF1A202C)
val LightInverseOnSurface = Color(0xFFEDF2F7)
val LightInversePrimary = Color(0xFFA5C8FF)
val LightSurfaceTint = PoliMiNavy

// ── dark theme (oled optimized) ──────────────────────────────────────
val DarkBackground = Color(0xFF000000) // pure black for oled
val DarkSurface = Color(0xFF11141A)    // anthracite with blue tint
val DarkSurfaceVariant = Color(0xFF1A1D24) // slightly lighter anthracite
val DarkOnBackground = Color(0xFFE2E8F0)
val DarkOnSurface = Color(0xFFE2E8F0)
val DarkOnSurfaceVariant = Color(0xFFA0AEC0)
val DarkOutline = Color(0xFF718096)
val DarkOutlineVariant = Color(0xFF4A5568)
val DarkInverseSurface = Color(0xFFEDF2F7)
val DarkInverseOnSurface = Color(0xFF1A202C)
val DarkInversePrimary = PoliMiNavy
val DarkSurfaceTint = PoliMiBrightBlueLight

// ── colors on-* (light) ─────────────────────────────────────────────
val OnPrimaryLight = Color(0xFFFFFFFF)
val OnPrimaryContainerLight = Color(0xFF001B3D)
val PrimaryContainerLight = Color(0xFFD6E3FF)
val OnSecondaryLight = Color(0xFFFFFFFF)
val OnSecondaryContainerLight = Color(0xFF001F2A)
val SecondaryContainerLight = Color(0xFFBFE9FF)
val OnTertiaryLight = Color(0xFFFFFFFF)
val OnTertiaryContainerLight = Color(0xFF410002)
val TertiaryContainerLight = Color(0xFFFFDAD6)

// ── colors on-* (dark) ──────────────────────────────────────────────
val OnPrimaryDark = Color(0xFF003062)
val OnPrimaryContainerDark = Color(0xFFD6E3FF)
val PrimaryContainerDark = Color(0xFF00468A)
val OnSecondaryDark = Color(0xFF003549)
val OnSecondaryContainerDark = Color(0xFFBFE9FF)
val SecondaryContainerDark = Color(0xFF004C68)
val OnTertiaryDark = Color(0xFF690005)
val OnTertiaryContainerDark = Color(0xFFFFDAD6)
val TertiaryContainerDark = Color(0xFF93000A)

// ── file type semantic colors ────────────────────────────────────────
val ColorPdf = Color(0xFFE53935)
val ColorWord = Color(0xFF1E88E5)
val ColorExcel = Color(0xFF43A047)
val ColorPowerPoint = Color(0xFFFB8C00)
val ColorZip = Color(0xFF8E24AA)
val ColorFolder = Color(0xFFFFB300)
val ColorLink = Color(0xFF039BE5)
val ColorGenericFile = Color(0xFF757575)

// ── gradients & glassmorphism ────────────────────────────────────────
val GradientDarkStart = Color(0xFF102C53) // polimi heritage blue
val GradientDarkEnd = Color(0xFF0BA4FF)   // polimi alpha blue
val GradientOverlayStart = Color(0xD9000000) // 85% opacity black
val GradientOverlayEnd = Color(0x26000000)   // 15% opacity black

val GlassWhite = Color(0x2AFFFFFF) // highly translucent white for pills
val GlassDark = Color(0x40000000)
val GlassSurfaceLight = Color(0xB3FFFFFF) // 70% opacity
val GlassSurfaceDark = Color(0xB3161B22)

// ── shimmer ──────────────────────────────────────────────────────────
val ShimmerLightBase = Color(0xFFE0E0E0)
val ShimmerLightHighlight = Color(0xFFF5F5F5)
val ShimmerDarkBase = Color(0xFF2A2A2A)
val ShimmerDarkHighlight = Color(0xFF3D3D3D)
