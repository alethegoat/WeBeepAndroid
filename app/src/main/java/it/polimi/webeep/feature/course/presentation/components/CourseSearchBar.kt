package it.polimi.webeep.feature.course.presentation.components

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import it.polimi.webeep.R
import it.polimi.webeep.core.ui.components.WeBeepSearchField
import it.polimi.webeep.core.ui.theme.WeBeepSpacing

/** Search field of the materials list, with the spacing the list gives it. */
@Composable
internal fun CourseSearchBar(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    WeBeepSearchField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = stringResource(R.string.search_materials_placeholder),
        modifier = modifier.padding(
            start = WeBeepSpacing.md,
            end = WeBeepSpacing.md,
            top = WeBeepSpacing.sm,
            bottom = WeBeepSpacing.xxs
        )
    )
}
