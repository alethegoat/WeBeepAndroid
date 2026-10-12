package it.polimi.webeep.feature.settings.presentation.components

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import it.polimi.webeep.R
import it.polimi.webeep.core.ui.components.GroupPosition
import it.polimi.webeep.core.ui.components.GroupedListGap
import it.polimi.webeep.core.ui.components.GroupedListItem
import it.polimi.webeep.core.ui.theme.WeBeepSpacing
import it.polimi.webeep.feature.settings.domain.model.SyncFrequency

/** Wi-Fi only, how often to check, and the manual "check now". */
@Composable
fun SyncSection(
    wifiOnlySync: Boolean,
    syncFrequency: SyncFrequency,
    lastCheckAt: Long?,
    checkInProgress: Boolean,
    onWifiOnlyChange: (Boolean) -> Unit,
    onFrequencySelect: (SyncFrequency) -> Unit,
    onCheckNowClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(GroupedListGap)) {
        SettingsSectionTitle(stringResource(R.string.settings_section_sync))
        GroupedListItem(position = GroupPosition.TOP) {
            SettingsSwitchItem(
                icon = Icons.Rounded.Wifi,
                title = stringResource(R.string.settings_wifi_only_title),
                subtitle = stringResource(R.string.settings_wifi_only_subtitle),
                checked = wifiOnlySync,
                onCheckedChange = onWifiOnlyChange
            )
        }
        GroupedListItem(position = GroupPosition.MIDDLE) {
            SyncFrequencyItem(selectedFrequency = syncFrequency, onFrequencySelect = onFrequencySelect)
        }
        GroupedListItem(position = GroupPosition.BOTTOM) {
            LastCheckItem(
                lastCheckAt = lastCheckAt,
                checkInProgress = checkInProgress,
                onCheckNowClick = onCheckNowClick
            )
        }
    }
}

@Composable
private fun SyncFrequencyItem(selectedFrequency: SyncFrequency, onFrequencySelect: (SyncFrequency) -> Unit) {
    val options = listOf(
        SyncFrequency.FIFTEEN_MINUTES to stringResource(R.string.settings_sync_freq_15m),
        SyncFrequency.ONE_HOUR to stringResource(R.string.settings_sync_freq_1h),
        SyncFrequency.TWELVE_HOURS to stringResource(R.string.settings_sync_freq_12h),
        SyncFrequency.DAILY to stringResource(R.string.settings_sync_freq_daily),
        SyncFrequency.NEVER to stringResource(R.string.settings_sync_freq_never)
    )
    SettingsDropdownRow(
        icon = Icons.Rounded.Sync,
        title = stringResource(R.string.update_frequency),
        options = options,
        selected = options.firstOrNull { it.first == selectedFrequency }?.second.orEmpty(),
        onSelect = onFrequencySelect
    )
}

@Composable
private fun LastCheckItem(lastCheckAt: Long?, checkInProgress: Boolean, onCheckNowClick: () -> Unit) {
    val status = when {
        checkInProgress -> stringResource(R.string.settings_check_started)
        lastCheckAt != null -> stringResource(
            R.string.settings_last_check_at,
            DateUtils.getRelativeTimeSpanString(lastCheckAt, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)
        )
        else -> stringResource(R.string.settings_last_check_never)
    }
    SettingsRow(
        icon = Icons.Rounded.Notifications,
        title = stringResource(R.string.settings_last_check_title),
        subtitle = status
    ) {
        TextButton(
            onClick = onCheckNowClick,
            enabled = !checkInProgress,
            modifier = Modifier.heightIn(min = WeBeepSpacing.touchTarget)
        ) {
            Text(stringResource(R.string.settings_check_now))
        }
    }
}
