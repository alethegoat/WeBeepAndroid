package it.polimi.webeep.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Extended colors not covered by Material 3 roles. Always provided by
 * [WeBeepTheme], also when dynamic color is on.
 */
@Immutable
data class WeBeepColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val filePdf: Color,
    val fileWord: Color,
    val fileExcel: Color,
    val filePowerPoint: Color,
    val fileZip: Color,
    val fileFolder: Color,
    val fileLink: Color,
    val fileGeneric: Color,
    /** Base color for overlays on images/video; apply alpha at the call site. */
    val mediaScrim: Color,
    val onMedia: Color,
    val favourite: Color,
    /** Base color for glass borders/highlights; apply alpha at the call site. */
    val glassHighlight: Color,
    val auroraPrimary: Color,
    val auroraSecondary: Color,
    val auroraTertiary: Color,
)

val LightWeBeepColors = WeBeepColors(
    success = SuccessLight,
    onSuccess = OnSuccessLight,
    successContainer = SuccessContainerLight,
    onSuccessContainer = OnSuccessContainerLight,
    warning = WarningLight,
    onWarning = OnWarningLight,
    filePdf = ColorPdf,
    fileWord = ColorWord,
    fileExcel = ColorExcel,
    filePowerPoint = ColorPowerPoint,
    fileZip = ColorZip,
    fileFolder = ColorFolder,
    fileLink = ColorLink,
    fileGeneric = ColorGenericFile,
    mediaScrim = MediaScrim,
    onMedia = OnMedia,
    favourite = FavouriteGold,
    glassHighlight = GlassHighlight,
    auroraPrimary = AuroraPrimaryLight,
    auroraSecondary = AuroraSecondaryLight,
    auroraTertiary = AuroraTertiaryLight,
)

val DarkWeBeepColors = WeBeepColors(
    success = SuccessDark,
    onSuccess = OnSuccessDark,
    successContainer = SuccessContainerDark,
    onSuccessContainer = OnSuccessContainerDark,
    warning = WarningDark,
    onWarning = OnWarningDark,
    filePdf = ColorPdfDark,
    fileWord = ColorWordDark,
    fileExcel = ColorExcelDark,
    filePowerPoint = ColorPowerPointDark,
    fileZip = ColorZipDark,
    fileFolder = ColorFolderDark,
    fileLink = ColorLinkDark,
    fileGeneric = ColorGenericFileDark,
    mediaScrim = MediaScrim,
    onMedia = OnMedia,
    favourite = FavouriteGold,
    glassHighlight = GlassHighlight,
    auroraPrimary = AuroraPrimaryDark,
    auroraSecondary = AuroraSecondaryDark,
    auroraTertiary = AuroraTertiaryDark,
)

val LocalWeBeepColors = staticCompositionLocalOf { LightWeBeepColors }
