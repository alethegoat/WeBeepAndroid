package it.polimi.webeep.core.ui.theme

import androidx.compose.ui.graphics.Color

// ── primary (polimi heritage blue / webeep teal) ──────────────
val PoliMiNavy = Color(0xFF0A3366)
val PoliMiNavyLight = Color(0xFF164B8E)
val PoliMiNavyDark = Color(0xFF041C3D)

// ── secondary (polimi alpha blue / cyan) ───────────────────────
val PoliMiBrightBlue = Color(0xFF0091FF)
val PoliMiBrightBlueLight = Color(0xFF00C2FF)
val PoliMiBrightBlueDark = Color(0xFF0066CC)

// ── tertiary / accent ────────────────────────────────────────────────
val PoliMiRed = Color(0xFFE63946)
val PoliMiRedLight = Color(0xFFFF6B6B)
val PoliMiRedDark = Color(0xFFB71C2A)

// ── error ────────────────────────────────────────────────────────────
val ErrorRed = Color(0xFFE04040)
val ErrorRedDark = Color(0xFFFF6B6B)
val OnErrorLight = Color(0xFFFFFFFF)
val OnErrorDark = Color(0xFF3B0000)
val ErrorContainerLight = Color(0xFFFFE0E0)
val ErrorContainerDark = Color(0xFF5C0000)
val OnErrorContainerLight = Color(0xFF4C0000)
val OnErrorContainerDark = Color(0xFFFFE0E0)

// ── light theme ──────────────────────────────────────────────────────
// A touch darker than white surfaces, so cards and grouped rows stand out.
val LightBackground = Color(0xFFF1F4F8)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFEDF1F5)
val LightOnBackground = Color(0xFF11141A)
val LightOnSurface = Color(0xFF11141A)
val LightOnSurfaceVariant = Color(0xFF4A5568)
val LightOutline = Color(0xFFA0AEC0)
val LightOutlineVariant = Color(0xFFCBD5E0)
val LightInverseSurface = Color(0xFF1A202C)
val LightInverseOnSurface = Color(0xFFEDF2F7)
val LightInversePrimary = Color(0xFFA5C8FF)
val LightSurfaceTint = PoliMiNavy
val LightSurfaceDim = Color(0xFFD8DEE7)
val LightSurfaceBright = Color(0xFFF7F9FC)
val LightSurfaceContainerLowest = Color(0xFFFFFFFF)
val LightSurfaceContainerLow = Color(0xFFF1F4F9)
val LightSurfaceContainer = Color(0xFFEBEFF5)
val LightSurfaceContainerHigh = Color(0xFFE5EAF1)
val LightSurfaceContainerHighest = Color(0xFFDFE5ED)
val LightScrim = Color(0xFF000000)

// ── dark theme (oled optimized) ──────────────────────────────────────
val DarkBackground = Color(0xFF000000)
val DarkSurface = Color(0xFF11141A)
val DarkSurfaceVariant = Color(0xFF1A1D24)
val DarkOnBackground = Color(0xFFE2E8F0)
val DarkOnSurface = Color(0xFFE2E8F0)
val DarkOnSurfaceVariant = Color(0xFFA0AEC0)
val DarkOutline = Color(0xFF718096)
val DarkOutlineVariant = Color(0xFF4A5568)
val DarkInverseSurface = Color(0xFFEDF2F7)
val DarkInverseOnSurface = Color(0xFF1A202C)
val DarkInversePrimary = PoliMiNavy
val DarkSurfaceTint = PoliMiBrightBlueLight
val DarkSurfaceDim = Color(0xFF0B0D12)
val DarkSurfaceBright = Color(0xFF2D323C)
val DarkSurfaceContainerLowest = Color(0xFF050608)
val DarkSurfaceContainerLow = Color(0xFF0E1116)
val DarkSurfaceContainer = Color(0xFF151920)
val DarkSurfaceContainerHigh = Color(0xFF1C2029)
val DarkSurfaceContainerHighest = Color(0xFF262B35)
val DarkScrim = Color(0xFF000000)

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
// Teal secondary keeps primary/secondary gradients visible on OLED black.
val SecondaryDark = Color(0xFF5ED6CE)
val OnPrimaryDark = Color(0xFF003062)
val OnPrimaryContainerDark = Color(0xFFD6E3FF)
val PrimaryContainerDark = Color(0xFF00468A)
val OnSecondaryDark = Color(0xFF003733)
val OnSecondaryContainerDark = Color(0xFFB3F2EB)
val SecondaryContainerDark = Color(0xFF00504A)
val OnTertiaryDark = Color(0xFF690005)
val OnTertiaryContainerDark = Color(0xFFFFDAD6)
val TertiaryContainerDark = Color(0xFF93000A)

// ── file type semantic colors (light) ────────────────────────────────
val ColorPdf = Color(0xFFE53935)
val ColorWord = Color(0xFF1E88E5)
val ColorExcel = Color(0xFF43A047)
val ColorPowerPoint = Color(0xFFFB8C00)
val ColorZip = Color(0xFF8E24AA)
val ColorFolder = Color(0xFFFFB300)
val ColorLink = Color(0xFF039BE5)
val ColorGenericFile = Color(0xFF757575)

// ── file type semantic colors (dark, desaturated) ────────────────────
val ColorPdfDark = Color(0xFFF28B82)
val ColorWordDark = Color(0xFF8AB4F8)
val ColorExcelDark = Color(0xFF81C995)
val ColorPowerPointDark = Color(0xFFFCAD70)
val ColorZipDark = Color(0xFFC58AF9)
val ColorFolderDark = Color(0xFFFDD663)
val ColorLinkDark = Color(0xFF78D0F5)
val ColorGenericFileDark = Color(0xFFA0AEC0)

// ── status ───────────────────────────────────────────────────────────
val SuccessLight = Color(0xFF2E7D32)
val OnSuccessLight = Color(0xFFFFFFFF)
val SuccessContainerLight = Color(0xFFC8E6C9)
val OnSuccessContainerLight = Color(0xFF0B3D0F)
val WarningLight = Color(0xFFB26A00)
val OnWarningLight = Color(0xFFFFFFFF)

val SuccessDark = Color(0xFF7BD88A)
val OnSuccessDark = Color(0xFF00390F)
val SuccessContainerDark = Color(0xFF0F4D1C)
val OnSuccessContainerDark = Color(0xFFB9F0C0)
val WarningDark = Color(0xFFFFB74D)
val OnWarningDark = Color(0xFF3F2400)

// ── media overlays, favourite, aurora glows ──────────────────────────
val MediaScrim = Color(0xFF000000)
val OnMedia = Color(0xFFFFFFFF)
val FavouriteGold = Color(0xFFFFD700)
val GlassHighlight = Color(0xFFFFFFFF)

val AuroraPrimaryLight = Color(0xFF8BB6FF)
val AuroraSecondaryLight = Color(0xFF6EC8FF)
val AuroraTertiaryLight = Color(0xFFFFC1D6)
val AuroraPrimaryDark = Color(0xFF1B6FD6)
val AuroraSecondaryDark = Color(0xFF00B7E0)
val AuroraTertiaryDark = Color(0xFF1FA3A0)

// ── gradients & glassmorphism ────────────────────────────────────────
val GradientDarkStart = Color(0xFF102C53)
val GradientDarkEnd = Color(0xFF0BA4FF)
val GradientOverlayStart = Color(0xD9000000)
val GradientOverlayEnd = Color(0x26000000)

val GlassWhite = Color(0x2AFFFFFF)
val GlassDark = Color(0x40000000)
val GlassSurfaceLight = Color(0xB3FFFFFF)
val GlassSurfaceDark = Color(0xB3161B22)

// ── shimmer ──────────────────────────────────────────────────────────
val ShimmerLightBase = Color(0xFFE0E0E0)
val ShimmerLightHighlight = Color(0xFFF5F5F5)
val ShimmerDarkBase = Color(0xFF2A2A2A)
val ShimmerDarkHighlight = Color(0xFF3D3D3D)
