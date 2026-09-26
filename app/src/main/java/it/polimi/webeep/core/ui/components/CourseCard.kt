package it.polimi.webeep.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import it.polimi.webeep.R
import it.polimi.webeep.core.ui.theme.WeBeepSpacing
import it.polimi.webeep.core.ui.theme.WeBeepTheme
import it.polimi.webeep.core.ui.util.bouncyClickable
import it.polimi.webeep.core.ui.util.courseSharedBounds
import it.polimi.webeep.core.util.CourseTitleParser
import java.util.Locale

/** UI model for a course card. [progress] is 0f–1f, null if unavailable. */
@Immutable
data class CourseUiModel(
    val id: Int,
    val shortname: String,
    val fullname: String,
    val courseImage: String? = null,
    val progress: Float? = null,
    val isFavourite: Boolean = false,
    val category: String? = null,
    val endDate: Long = 0L,
)

/** Height of the cover image when a course has one; kept small so text stays the focus. */
private val CourseImageHeight = 112.dp

/** Size of the monogram tile shown when a course has no image. */
private val CourseMonogramSize = 56.dp

/** Display-ready pieces of a Moodle course title. */
@Immutable
private data class CourseCardText(
    val code: String,
    val name: String,
    val subtitle: String,
    val initials: String,
)

@Composable
fun CourseCard(
    course: CourseUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFavouriteClick: (() -> Unit)? = null,
) {
    val openLabel = stringResource(R.string.cd_open_course)
    val text = remember(course.fullname, course.shortname, course.category) {
        course.toCardText()
    }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .courseSharedBounds(course.id)
            .bouncyClickable(
                role = Role.Button,
                onClickLabel = openLabel
            ) { onClick() },
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        val image = course.courseImage
        if (image.isNullOrBlank()) {
            CompactCourseContent(course = course, text = text, onFavouriteClick = onFavouriteClick)
        } else {
            CourseCover(image = image, course = course, onFavouriteClick = onFavouriteClick)
            CourseInfo(
                text = text,
                progress = course.progress,
                modifier = Modifier.padding(WeBeepSpacing.md)
            )
        }
    }
}

@Composable
private fun CompactCourseContent(
    course: CourseUiModel,
    text: CourseCardText,
    onFavouriteClick: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = WeBeepSpacing.md,
                top = WeBeepSpacing.md,
                bottom = WeBeepSpacing.md,
                end = if (onFavouriteClick != null) WeBeepSpacing.xxs else WeBeepSpacing.md
            ),
        verticalAlignment = Alignment.Top,
    ) {
        CourseMonogram(key = text.code.ifBlank { course.fullname }, initials = text.initials)
        Spacer(modifier = Modifier.width(WeBeepSpacing.md))
        CourseInfo(
            text = text,
            progress = course.progress,
            modifier = Modifier.weight(1f)
        )
        if (onFavouriteClick != null) {
            FavouriteButton(
                isFavourite = course.isFavourite,
                onClick = onFavouriteClick,
                onMedia = false
            )
        }
    }
}

@Composable
private fun CourseCover(
    image: String,
    course: CourseUiModel,
    onFavouriteClick: (() -> Unit)?,
) {
    val scrim = WeBeepTheme.colors.mediaScrim
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(CourseImageHeight)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
    ) {
        AsyncImage(
            model = image,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
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
private fun CourseInfo(
    text: CourseCardText,
    progress: Float?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        if (text.code.isNotBlank()) {
            Text(
                text = text.code,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
            )
            Spacer(modifier = Modifier.height(WeBeepSpacing.xxs))
        }
        Text(
            text = text.name,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (text.subtitle.isNotBlank()) {
            Spacer(modifier = Modifier.height(WeBeepSpacing.xxs))
            Text(
                text = text.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
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
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
        }
    }
}

/** Deterministic, theme-coloured tile with the course initials (icon if none). */
@Composable
private fun CourseMonogram(key: String, initials: String) {
    val scheme = MaterialTheme.colorScheme
    val palette = listOf(
        scheme.primaryContainer to scheme.onPrimaryContainer,
        scheme.secondaryContainer to scheme.onSecondaryContainer,
        scheme.tertiaryContainer to scheme.onTertiaryContainer,
    )
    val (container, content) = palette[Math.floorMod(key.hashCode(), palette.size)]
    Box(
        modifier = Modifier
            .size(CourseMonogramSize)
            .clip(MaterialTheme.shapes.medium)
            .background(container),
        contentAlignment = Alignment.Center,
    ) {
        if (initials.isNotEmpty()) {
            Text(
                text = initials,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = content,
                maxLines = 1,
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.School,
                contentDescription = null,
                tint = content,
            )
        }
    }
}

@Composable
private fun FavouriteButton(
    isFavourite: Boolean,
    onClick: () -> Unit,
    onMedia: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = WeBeepTheme.colors
    val idleTint = if (onMedia) colors.onMedia else MaterialTheme.colorScheme.onSurfaceVariant
    IconButton(
        onClick = onClick,
        modifier = modifier,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = if (onMedia) colors.mediaScrim.copy(alpha = 0.4f) else Color.Transparent,
        ),
    ) {
        Icon(
            imageVector = if (isFavourite) Icons.Filled.Star else Icons.Outlined.StarOutline,
            contentDescription = if (isFavourite) {
                stringResource(R.string.cd_remove_fav)
            } else {
                stringResource(R.string.cd_add_fav)
            },
            tint = if (isFavourite) colors.favourite else idleTint,
        )
    }
}

private val InitialsStopWords = setOf(
    "and", "for", "the", "with", "of", "in", "to",
    "di", "del", "della", "dei", "delle", "degli", "e", "per", "con", "la", "il", "lo", "le", "gli",
)

private fun CourseUiModel.toCardText(): CourseCardText {
    val parsed = CourseTitleParser.parse(fullname)
    val code = parsed.code.orEmpty().trim()
    val name = parsed.name.trim().ifBlank { fullname.trim() }.toTitleCaseIfShouting()
    val subtitle = listOfNotNull(parsed.teacher, parsed.academicYear)
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .ifEmpty {
            listOfNotNull(category?.trim()?.takeIf { it.isNotEmpty() && !it.all(Char::isDigit) })
        }
        .joinToString(" · ")
    return CourseCardText(code = code, name = name, subtitle = subtitle, initials = initialsOf(name))
}

private fun initialsOf(name: String): String {
    val words = name.split(Regex("[^\\p{L}\\p{N}]+")).filter { it.isNotEmpty() }
    val meaningful = words.filter { it.lowercase(Locale.ROOT) !in InitialsStopWords }.ifEmpty { words }
    return meaningful
        .take(2)
        .joinToString("") { it.first().uppercase(Locale.getDefault()) }
}

/** Moodle titles often arrive in ALL CAPS: soften them to Title Case, leave mixed case alone. */
private fun String.toTitleCaseIfShouting(): String {
    val letters = filter(Char::isLetter)
    if (letters.isEmpty() || letters != letters.uppercase(Locale.ROOT)) return this
    val locale = Locale.getDefault()
    return lowercase(locale)
        .split(" ")
        .joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
        }
}

// ── previews ─────────────────────────────────────────────────────────

@Preview(showBackground = true, name = "CourseCard – Senza immagine (Light)")
@Composable
private fun CourseCardCompactPreview() {
    WeBeepTheme(darkTheme = false) {
        Box(modifier = Modifier.padding(WeBeepSpacing.md)) {
            CourseCard(
                course = CourseUiModel(
                    id = 1,
                    shortname = "052537 - TECHNOLOGIES FOR INF... 889897",
                    fullname = "052537 - TECHNOLOGIES FOR INFORMATION SYSTEMS (TANCA LETIZIA) [2025-26]",
                    courseImage = null,
                    progress = 0.65f,
                    isFavourite = true,
                ),
                onClick = {},
                onFavouriteClick = {},
            )
        }
    }
}

@Preview(showBackground = true, name = "CourseCard – Con immagine")
@Composable
private fun CourseCardImagePreview() {
    WeBeepTheme(darkTheme = false) {
        Box(modifier = Modifier.padding(WeBeepSpacing.md)) {
            CourseCard(
                course = CourseUiModel(
                    id = 2,
                    shortname = "097683 - MAT",
                    fullname = "097683 - Analisi Matematica 1 (Rossi Mario) [2025-26]",
                    courseImage = "https://example.invalid/cover.png",
                    progress = null,
                ),
                onClick = {},
                onFavouriteClick = {},
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, name = "CourseCard – Dark")
@Composable
private fun CourseCardDarkPreview() {
    WeBeepTheme(darkTheme = true) {
        Box(modifier = Modifier.padding(WeBeepSpacing.md)) {
            CourseCard(
                course = CourseUiModel(
                    id = 3,
                    shortname = "054309 - LOC",
                    fullname = "054309 - LOCALIZATION, NAVIGATION AND SMART MOBILITY (NICOLI MONICA) [2025-26]",
                    courseImage = null,
                    progress = 0.3f,
                ),
                onClick = {},
            )
        }
    }
}
