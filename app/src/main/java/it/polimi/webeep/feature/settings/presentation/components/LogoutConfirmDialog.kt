package it.polimi.webeep.feature.settings.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import it.polimi.webeep.R
import it.polimi.webeep.core.ui.theme.WeBeepTheme

/** Asks before ending the session, since logging out wipes the local data. */
@Composable
fun LogoutConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        icon = {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Logout,
                contentDescription = null
            )
        },
        title = { Text(stringResource(R.string.settings_logout_dialog_title)) },
        text = { Text(stringResource(R.string.settings_logout_dialog_message)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(R.string.settings_logout_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_logout_dialog_cancel))
            }
        }
    )
}

@Preview(name = "Logout dialog")
@Composable
private fun LogoutConfirmDialogPreview() {
    WeBeepTheme {
        LogoutConfirmDialog(onConfirm = {}, onDismiss = {})
    }
}
