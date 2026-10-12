package it.polimi.webeep.feature.course.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import it.polimi.webeep.R
import it.polimi.webeep.core.ui.components.GroupedListItem
import it.polimi.webeep.core.ui.theme.WeBeepSpacing
import it.polimi.webeep.feature.course.presentation.CourseListItem

/** Tappable title of a course section; its rows follow it while expanded. */
@Composable
internal fun SectionHeaderRow(item: CourseListItem.SectionHeader, onClick: () -> Unit, modifier: Modifier = Modifier) {
    GroupedListItem(position = item.position, modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onClick)
                .padding(
                    start = WeBeepSpacing.md,
                    end = WeBeepSpacing.xxs,
                    top = WeBeepSpacing.sm,
                    bottom = WeBeepSpacing.sm
                )
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() }
                )
                if (item.summary != null) {
                    Text(
                        text = item.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = WeBeepSpacing.xxs)
                    )
                }
            }
            ExpandChevron(
                expanded = item.isExpanded,
                contentDescription = stringResource(
                    if (item.isExpanded) R.string.cd_collapse_section else R.string.cd_expand_section
                )
            )
        }
    }
}

@Composable
internal fun SectionEmptyRow(item: CourseListItem.SectionEmpty, modifier: Modifier = Modifier) {
    GroupedListItem(position = item.position, modifier = modifier) {
        Text(
            text = stringResource(R.string.no_materials_section),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = WeBeepSpacing.md, vertical = WeBeepSpacing.md)
        )
    }
}
