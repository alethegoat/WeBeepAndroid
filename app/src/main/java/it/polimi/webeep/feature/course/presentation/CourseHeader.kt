package it.polimi.webeep.feature.course.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import it.polimi.webeep.R
import it.polimi.webeep.core.ui.theme.WeBeepSpacing
import it.polimi.webeep.core.util.CourseTitle

/**
 * Compact course header: the human-readable course name (max 2 lines) and a
 * wrapping row of labels for code, teacher and academic year.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CourseHeader(title: CourseTitle, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title.name,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.semantics { heading() }
        )
        val hasMeta = title.code != null || title.teacher != null || title.academicYear != null
        if (hasMeta) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(WeBeepSpacing.xs),
                verticalArrangement = Arrangement.spacedBy(WeBeepSpacing.xs),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = WeBeepSpacing.sm)
            ) {
                title.code?.let { code ->
                    MetaLabel(
                        icon = Icons.Rounded.Tag,
                        text = code,
                        description = stringResource(R.string.course_meta_code, code)
                    )
                }
                title.teacher?.let { teacher ->
                    MetaLabel(
                        icon = Icons.Rounded.Person,
                        text = teacher,
                        description = stringResource(R.string.course_meta_teacher, teacher)
                    )
                }
                title.academicYear?.let { year ->
                    MetaLabel(
                        icon = Icons.Rounded.DateRange,
                        text = year,
                        description = stringResource(R.string.course_meta_year, year)
                    )
                }
            }
        }
    }
}

/** Non-interactive assist-style label (not a chip: it has no click action). */
@Composable
private fun MetaLabel(icon: ImageVector, text: String, description: String, modifier: Modifier = Modifier) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.clearAndSetSemantics { contentDescription = description }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WeBeepSpacing.xs),
            modifier = Modifier.padding(horizontal = WeBeepSpacing.sm, vertical = WeBeepSpacing.xs)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(AssistChipDefaults.IconSize)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
