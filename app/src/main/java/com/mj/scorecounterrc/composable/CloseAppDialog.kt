package com.mj.scorecounterrc.composable

import android.content.Intent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.mj.scorecounterrc.ble.ConnectionManager
import com.mj.scorecounterrc.service.RcService
import com.mj.scorecounterrc.util.findActivity
import timber.log.Timber

@Composable
fun CloseAppDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context.findActivity()

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                Timber.d("Closing the app with back button.")
                context.stopService(Intent(context, RcService::class.java))
                ConnectionManager.disconnectAllDevices()
                activity.finishAndRemoveTask()
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