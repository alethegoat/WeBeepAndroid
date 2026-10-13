package it.polimi.webeep.feature.dashboard.presentation.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import it.polimi.webeep.R
import it.polimi.webeep.core.ui.components.groupedListContainerColor
import it.polimi.webeep.core.ui.theme.WeBeepSpacing
import it.polimi.webeep.core.ui.util.bouncyClickable
import it.polimi.webeep.core.ui.util.courseSharedBounds
import it.polimi.webeep.feature.dashboard.presentation.CourseUiModel

@Composable
fun CourseCard(course: CourseUiModel, onClick: () -> Unit, modifier: Modifier = Modifier, onFavouriteClick: (() -> Unit)? = null) {
    val openLabel = stringResource(R.string.cd_open_course)
    val text = remember(course.fullname, course.shortname, course.category) {
        course.toCardText()
    }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .courseSharedBounds(course.id, isScreen = false)
            .bouncyClickable(
                role = Role.Button,
                onClickLabel = openLabel
            ) { onClick() },
        shape = MaterialTheme.shapes.large,
        // Tonal only (no outline): same surface as the grouped lists elsewhere.
        colors = CardDefaults.cardColors(
            containerColor = groupedListContainerColor(),
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
private fun CompactCourseContent(course: CourseUiModel, text: CourseCardText, onFavouriteClick: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = WeBeepSpacing.md,
                top = WeBeepSpacing.md,
                bottom = WeBeepSpacing.md,
                end = if (onFavouriteClick != null) WeBeepSpacing.xxs else WeBeepSpacing.md
            ),
        verticalAlignment = Alignment.Top
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
