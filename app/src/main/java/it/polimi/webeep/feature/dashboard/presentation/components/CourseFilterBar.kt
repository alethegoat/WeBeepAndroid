package it.polimi.webeep.feature.dashboard.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import it.polimi.webeep.R
import it.polimi.webeep.core.ui.theme.WeBeepMotion
import it.polimi.webeep.core.ui.theme.WeBeepSpacing
import it.polimi.webeep.core.ui.util.LocalReduceMotion
import it.polimi.webeep.core.ui.util.bouncyClickable
import it.polimi.webeep.feature.dashboard.presentation.CourseFilter
import it.polimi.webeep.feature.dashboard.presentation.components.CourseCard
import kotlinx.coroutines.flow.filter

@Composable
fun CourseFilterBar(selected: CourseFilter, onSelect: (CourseFilter) -> Unit, modifier: Modifier = Modifier) {
    val reduceMotion = LocalReduceMotion.current
    val haptics = LocalHapticFeedback.current
    val filters = listOf(
        CourseFilter.ACTIVE to stringResource(R.string.filter_active),
        CourseFilter.PAST to stringResource(R.string.filter_past),
        CourseFilter.ALL to stringResource(R.string.filter_all)
    )
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(WeBeepSpacing.xxs)
    ) {
        val pillWidth = maxWidth / filters.size
        val selectedIndex = filters.indexOfFirst { it.first == selected }
        val pillOffset by animateDpAsState(
            targetValue = pillWidth * selectedIndex,
            animationSpec = WeBeepMotion.softSpring(reduceMotion),
            label = "pillOffset"
        )

        Box(
            modifier = Modifier
                .offset { IntOffset(pillOffset.roundToPx(), 0) }
                .width(pillWidth)
                .height(WeBeepSpacing.touchTarget)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.primary)
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            filters.forEach { (filter, label) ->
                val isSelected = selected == filter
                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    animationSpec = WeBeepMotion.smooth(reduceMotion = reduceMotion),
                    label = "filterColor"
                )
                Box(
                    modifier = Modifier
                        .width(pillWidth)
                        .height(WeBeepSpacing.touchTarget)
                        .semantics {
                            role = Role.Tab
                            this.selected = isSelected
                        }
                        .bouncyClickable(
                            role = Role.Tab,
                            onClickLabel = label
                        ) {
                            if (!isSelected) {
                                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                onSelect(filter)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = contentColor
                    )
                }
            }
        }
    }
}
