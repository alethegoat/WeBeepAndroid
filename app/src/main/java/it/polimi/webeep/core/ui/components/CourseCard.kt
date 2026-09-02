package it.polimi.webeep.core.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
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
import it.polimi.webeep.core.ui.theme.GradientDarkEnd
import it.polimi.webeep.core.ui.theme.GradientDarkStart
import it.polimi.webeep.core.ui.theme.WeBeepTheme
import it.polimi.webeep.core.ui.util.bouncyClickable
import it.polimi.webeep.core.ui.util.courseSharedBounds

/**
 * ui model for a course card.
 *
 * @property id unique moodle course id.
 * @property shortname short course name (e.g. "info-2").
 * @property fullname full course name.
 * @property courseImage course cover image url, if available.
 * @property progress course progress (0f–1f), null if not available.
 * @property isFavourite whether the course is among the user's favorites.
 * @property category category or subject area of the course.
 */
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

/**
 * premium card for displaying a course.
 *
 * features:
 * - cover image loaded with coil ([asyncimage]) with dark gradient overlay.
 * - if image is not available, shows a polimi gradient (navy → blue).
 * - white title with shadow for readability on any background.
 * - semi-transparent subtitle (shortname).
 * - category chip on the top left.
 * - favorite star icon on the top right.
 * - progress bar at the bottom (optional).
 * - animated elevation on touch ("lift" effect).
 *
 * @param course course data to display.
 * @param onClick callback on card click.
 * @param onFavouriteClick callback on favorite icon click.
 * @param modifier external modifier.
 */
@Composable
fun CourseCard(
    course: CourseUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFavouriteClick: (() -> Unit)? = null,
) {
    val openLabel = stringResource(R.string.cd_open_course)
    val cardShape = RoundedCornerShape(24.dp)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(228.dp)
            .courseSharedBounds(course.id)
            .shadow(8.dp, cardShape, clip = false, ambientColor = Color.Black.copy(alpha = 0.08f))
            .bouncyClickable(
                role = Role.Button,
                onClickLabel = openLabel
            ) { onClick() },
        shape = cardShape,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── upper half: image or pattern ───────────────
            val gradientBrush = remember {
                Brush.linearGradient(
                    colors = listOf(GradientDarkStart, GradientDarkEnd),
                    start = Offset.Zero,
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.5f)
                    .background(gradientBrush)
            ) {
                if (course.courseImage != null) {
                    AsyncImage(
                        model = course.courseImage,
                        contentDescription = course.fullname,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Black.copy(alpha = 0.08f),
                                1f to Color.Black.copy(alpha = 0.28f)
                            )
                        )
                )

                // category and star in overlay on image
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    val category = course.category
                    val showCategory = category != null && category.isNotBlank() && !category.all { it.isDigit() }
                    if (showCategory) {
                        CategoryChip(category = category)
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    if (onFavouriteClick != null) {
                        IconButton(
                            onClick = onFavouriteClick,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.3f)),
                        ) {
                            Icon(
                                imageVector = if (course.isFavourite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                contentDescription = if (course.isFavourite) stringResource(R.string.cd_remove_fav) else stringResource(R.string.cd_add_fav),
                                tint = if (course.isFavourite) Color(0xFFFFD700) else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // ── lower half: content ────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.5f)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = course.fullname,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = course.shortname,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (course.progress != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { course.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    )
                }
            }
        }
    }
}

/**
 * compact chip for course category, with semi-transparent background.
 */
@Composable
private fun CategoryChip(
    category: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.45f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = category,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.9f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ── previews ─────────────────────────────────────────────────────────

@Preview(showBackground = true, name = "CourseCard – Con Immagine (Light)")
@Composable
private fun CourseCardWithImagePreview() {
    WeBeepTheme(darkTheme = false) {
        Box(modifier = Modifier.padding(16.dp)) {
            CourseCard(
                course = CourseUiModel(
                    id = 1,
                    shortname = "051234 - INFO",
                    fullname = "Informatica e Elementi di Informatica Medica",
                    courseImage = null, // in preview we don't load network
                    progress = 0.65f,
                    isFavourite = true,
                    category = "Ingegneria",
                ),
                onClick = {},
                onFavouriteClick = {},
            )
        }
    }
}

@Preview(showBackground = true, name = "CourseCard – Senza Immagine (Gradient)")
@Composable
private fun CourseCardGradientPreview() {
    WeBeepTheme(darkTheme = false) {
        Box(modifier = Modifier.padding(16.dp)) {
            CourseCard(
                course = CourseUiModel(
                    id = 2,
                    shortname = "097683 - MAT",
                    fullname = "Analisi Matematica 1",
                    courseImage = null,
                    progress = null,
                    isFavourite = false,
                    category = "Scienze",
                ),
                onClick = {},
                onFavouriteClick = {},
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D1117, name = "CourseCard – Dark")
@Composable
private fun CourseCardDarkPreview() {
    WeBeepTheme(darkTheme = true) {
        Box(modifier = Modifier.padding(16.dp)) {
            CourseCard(
                course = CourseUiModel(
                    id = 3,
                    shortname = "056789 - FIS",
                    fullname = "Fisica Sperimentale",
                    courseImage = null,
                    progress = 0.3f,
                    isFavourite = true,
                    category = "Fisica",
                ),
                onClick = {},
                onFavouriteClick = {},
            )
        }
    }
}
