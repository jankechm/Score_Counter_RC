package com.mj.scorecounterrc.composable

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.mj.scorecounterrc.util.findActivity
import com.mj.scorecounterrc.viewmodel.CloseAppViewModel
import com.mj.scorecounterrc.viewmodel.CloseAppViewModel.CloseAppViewModelEvent

@Composable
fun CloseAppDialogRoot(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val closeAppViewModel = hiltViewModel<CloseAppViewModel>()

    CloseAppDialog(
        onDismiss = onDismiss,
        onCloseAppViewModelEvent = closeAppViewModel::onEvent,
        modifier = modifier
    )
}

@Composable
fun CloseAppDialog(
    onDismiss: () -> Unit,
    onCloseAppViewModelEvent: (CloseAppViewModelEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context.findActivity()

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onCloseAppViewModelEvent(CloseAppViewModelEvent.ConfirmButtonClickedEvent(activity))
            }) {
                Text("Yes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("No")
            }
        },
        title = {
            Text("Close application?")
        },
        text = {
            Text(
                text = "Do you really want to close the app?"
            )
        },
        modifier = modifier
    )
}