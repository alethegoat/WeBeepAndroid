package it.polimi.webeep.feature.dashboard.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.polimi.webeep.R
import it.polimi.webeep.core.ui.components.WeBeepSearchField
import it.polimi.webeep.core.ui.theme.WeBeepSpacing
import it.polimi.webeep.feature.dashboard.presentation.CourseFilter
import it.polimi.webeep.feature.dashboard.presentation.components.CourseCard

/** Search and filters never stretch wider than this. */
private val DashboardHeaderMaxWidth = 600.dp

private val OfflineIconSize = 16.dp

@Composable
fun DashboardListHeader(
    isOnline: Boolean,
    isCacheAvailable: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    courseFilter: CourseFilter,
    onFilterChange: (CourseFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    // Spans the whole grid row, but search and filters stay compact on tablets.
    Column(modifier = modifier.widthIn(max = DashboardHeaderMaxWidth)) {
        AnimatedVisibility(
            visible = !isOnline,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            OfflineBanner(isCacheAvailable = isCacheAvailable)
        }

        WeBeepSearchField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = stringResource(R.string.search_courses_placeholder),
            modifier = Modifier.padding(vertical = WeBeepSpacing.xxs)
        )

        CourseFilterBar(
            selected = courseFilter,
            onSelect = onFilterChange,
            modifier = Modifier.padding(vertical = WeBeepSpacing.xs)
        )
    }
}

@Composable
private fun OfflineBanner(isCacheAvailable: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = WeBeepSpacing.xxs)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(vertical = WeBeepSpacing.sm, horizontal = WeBeepSpacing.md)
    ) {
        Icon(
            imageVector = Icons.Rounded.CloudOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.size(OfflineIconSize)
        )
        Spacer(modifier = Modifier.width(WeBeepSpacing.xs))
        Text(
            text = stringResource(
                if (isCacheAvailable) R.string.offline_cache_message else R.string.offline_no_connection_message
            ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            fontWeight = FontWeight.Medium
        )
    }
}
