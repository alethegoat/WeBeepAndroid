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
import it.polimi.webeep.core.ui.theme.WeBeepSpacing
import it.polimi.webeep.core.ui.theme.WeBeepTheme
import it.polimi.webeep.core.ui.util.bouncyClickable
import it.polimi.webeep.core.ui.util.courseSharedBounds

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

@Composable
fun CourseCard(
    course: CourseUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFavouriteClick: (() -> Unit)? = null,
) {
    val openLabel = stringResource(R.string.cd_open_course)
    val cardShape = MaterialTheme.shapes.large
    val weBeepColors = WeBeepTheme.colors
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(228.dp)
            .courseSharedBounds(course.id)
            .shadow(8.dp, cardShape, clip = false, ambientColor = weBeepColors.mediaScrim.copy(alpha = 0.08f))
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
                                0f to weBeepColors.mediaScrim.copy(alpha = 0.08f),
                                1f to weBeepColors.mediaScrim.copy(alpha = 0.28f)
                            )
                        )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(WeBeepSpacing.xs),
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
                                .background(weBeepColors.mediaScrim.copy(alpha = 0.3f)),
                        ) {
                            Icon(
                                imageVector = if (course.isFavourite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                contentDescription = if (course.isFavourite) {
                                    stringResource(R.string.cd_remove_fav)
                                } else {
                                    stringResource(R.string.cd_add_fav)
                                },
                                tint = if (course.isFavourite) weBeepColors.favourite else weBeepColors.onMedia,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.5f)
                    .padding(horizontal = WeBeepSpacing.md, vertical = 14.dp),
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
                    Spacer(modifier = Modifier.height(WeBeepSpacing.xxs))
                    Text(
                        text = course.shortname,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (course.progress != null) {
                    Spacer(modifier = Modifier.height(WeBeepSpacing.xs))
                    LinearProgressIndicator(
                        progress = { course.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(
    category: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(WeBeepTheme.colors.mediaScrim.copy(alpha = 0.45f))
            .padding(horizontal = WeBeepSpacing.sm, vertical = 6.dp),
    ) {
        Text(
            text = category,
            style = MaterialTheme.typography.labelSmall,
            color = WeBeepTheme.colors.onMedia.copy(alpha = 0.9f),
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
        Box(modifier = Modifier.padding(WeBeepSpacing.md)) {
            CourseCard(
                course = CourseUiModel(
                    id = 1,
                    shortname = "051234 - INFO",
                    fullname = "Informatica e Elementi di Informatica Medica",
                    courseImage = null,
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
        Box(modifier = Modifier.padding(WeBeepSpacing.md)) {
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

@Preview(showBackground = true, backgroundColor = 0xFF000000, name = "CourseCard – Dark")
@Composable
private fun CourseCardDarkPreview() {
    WeBeepTheme(darkTheme = true) {
        Box(modifier = Modifier.padding(WeBeepSpacing.md)) {
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
