package it.polimi.webeep.feature.course.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Slideshow
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import it.polimi.webeep.core.ui.theme.WeBeepColors
import it.polimi.webeep.core.ui.theme.WeBeepMotion
import it.polimi.webeep.core.ui.theme.WeBeepSpacing
import it.polimi.webeep.core.ui.util.LocalReduceMotion

/** Size of the tinted square that hosts a file-type icon. */
internal val FileIconContainerSize = 40.dp

/** Standard Material icon size used inside rows and icon buttons. */
internal val RowIconSize = 24.dp

/** Indentation added for every nested folder level. */
private val FolderIndentStep = WeBeepSpacing.sm

/** Deeper levels stop indenting so long file names stay readable. */
private const val MAX_VISUAL_INDENT_LEVELS = 3

private const val FILE_ICON_BACKGROUND_ALPHA = 0.14f

internal fun indentFor(depth: Int): Dp = FolderIndentStep * depth.coerceIn(0, MAX_VISUAL_INDENT_LEVELS)

internal fun fileIconAndColor(filename: String, colors: WeBeepColors): Pair<ImageVector, Color> {
    val ext = filename.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "pdf" -> Icons.Rounded.PictureAsPdf to colors.filePdf
        "doc", "docx" -> Icons.Rounded.Description to colors.fileWord
        "xls", "xlsx" -> Icons.Rounded.TableChart to colors.fileExcel
        "ppt", "pptx" -> Icons.Rounded.Slideshow to colors.filePowerPoint
        "zip", "rar", "7z" -> Icons.Rounded.FolderZip to colors.fileZip
        else -> Icons.Rounded.Description to colors.fileGeneric
    }
}

/** Tinted rounded square with a file-type glyph; decorative, so no content description. */
@Composable
internal fun FileTypeIcon(icon: ImageVector, tint: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(FileIconContainerSize)
            .clip(MaterialTheme.shapes.small)
            .background(tint.copy(alpha = FILE_ICON_BACKGROUND_ALPHA)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(RowIconSize)
        )
    }
}

/** Chevron that rotates when [expanded] changes, instant when reduce-motion is on. */
@Composable
internal fun ExpandChevron(expanded: Boolean, contentDescription: String?, modifier: Modifier = Modifier) {
    val reduceMotion = LocalReduceMotion.current
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = WeBeepMotion.smooth(reduceMotion = reduceMotion),
        label = "chevron_rotation"
    )
    Box(
        modifier = modifier.size(WeBeepSpacing.touchTarget),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.ExpandMore,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(RowIconSize)
                .graphicsLayer { rotationZ = rotation }
        )
    }
}

/** Name (max 2 lines) plus optional metadata line, shared by module and file rows. */
@Composable
internal fun RowLabels(title: String, meta: String?, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (!meta.isNullOrBlank()) {
            Text(
                text = meta,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
