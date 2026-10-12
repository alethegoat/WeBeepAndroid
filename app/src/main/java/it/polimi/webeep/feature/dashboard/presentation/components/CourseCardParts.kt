package it.polimi.webeep.feature.dashboard.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import it.polimi.webeep.R
import it.polimi.webeep.core.ui.theme.WeBeepSpacing
import it.polimi.webeep.core.ui.theme.WeBeepTheme
import it.polimi.webeep.feature.dashboard.presentation.CourseUiModel

@Composable
internal fun CourseCover(image: String, course: CourseUiModel, onFavouriteClick: (() -> Unit)?) {
    val scrim = WeBeepTheme.colors.mediaScrim
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(CourseCardDimens.CoverHeight)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
    ) {
        AsyncImage(
            model = image,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to scrim.copy(alpha = 0.32f),
                        0.5f to Color.Transparent,
                        1f to scrim.copy(alpha = 0.12f)
                    )
                )
        )
        if (onFavouriteClick != null) {
            FavouriteButton(
                isFavourite = course.isFavourite,
                onClick = onFavouriteClick,
                onMedia = true,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(WeBeepSpacing.xxs)
            )
        }
    }
}

@Composable
internal fun CourseInfo(text: CourseCardText, progress: Float?, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        if (text.code.isNotBlank()) {
            Text(
                text = text.code,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(WeBeepSpacing.xxs))
        }
        Text(
            text = text.name,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (text.subtitle.isNotBlank()) {
            Spacer(modifier = Modifier.height(WeBeepSpacing.xxs))
            Text(
                text = text.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (progress != null) {
            Spacer(modifier = Modifier.height(WeBeepSpacing.sm))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
        }
    }
}

/** Deterministic, theme-coloured tile with the course initials (icon if none). */
@Composable
internal fun CourseMonogram(key: String, initials: String) {
    val scheme = MaterialTheme.colorScheme
    val palette = listOf(
        scheme.primaryContainer to scheme.onPrimaryContainer,
        scheme.secondaryContainer to scheme.onSecondaryContainer,
        scheme.tertiaryContainer to scheme.onTertiaryContainer
    )
    val (container, content) = palette[Math.floorMod(key.hashCode(), palette.size)]
    Box(
        modifier = Modifier
            .size(CourseCardDimens.MonogramSize)
            .clip(MaterialTheme.shapes.medium)
            .background(container),
        contentAlignment = Alignment.Center
    ) {
        if (initials.isNotEmpty()) {
            Text(
                text = initials,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = content,
                maxLines = 1
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.School,
                contentDescription = null,
                tint = content
            )
        }
    }
}

@Composable
internal fun FavouriteButton(isFavourite: Boolean, onClick: () -> Unit, onMedia: Boolean, modifier: Modifier = Modifier) {
    val colors = WeBeepTheme.colors
    val idleTint = if (onMedia) colors.onMedia else MaterialTheme.colorScheme.onSurfaceVariant
    IconButton(
        onClick = onClick,
        modifier = modifier,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = if (onMedia) colors.mediaScrim.copy(alpha = 0.4f) else Color.Transparent
        )
    ) {
        Icon(
            imageVector = if (isFavourite) Icons.Rounded.Star else Icons.Outlined.StarOutline,
            contentDescription = if (isFavourite) {
                stringResource(R.string.cd_remove_fav)
            } else {
                stringResource(R.string.cd_add_fav)
            },
            tint = if (isFavourite) colors.favourite else idleTint
        )
    }
}
