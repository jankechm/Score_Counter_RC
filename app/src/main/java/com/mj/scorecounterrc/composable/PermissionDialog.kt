package com.mj.scorecounterrc.composable

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun PermissionDialog(
    permissionTextProvider: PermissionTextProvider,
    isPermanentlyDeclined: Boolean,
    onDismiss: () -> Unit,
    onConfirmBtnClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onConfirmBtnClick) {
                Text(if (isPermanentlyDeclined) "Grant permissions" else "OK")
            }
        },
        title = {
            Text("Permissions required")
        },
        text = {
            Text(
                text = permissionTextProvider.getDescription(isPermanentlyDeclined)
            )
        },
        modifier = modifier
    )
}

interface PermissionTextProvider {
    fun getDescription(isPermanentlyDeclined: Boolean): String
}

class BluetoothScanPermissionTextProvider: PermissionTextProvider {
    override fun getDescription(isPermanentlyDeclined: Boolean): String {
        return if(isPermanentlyDeclined) {
            "It seems you permanently declined Bluetooth Scan permission. " +
                    "You can go to the app settings to grant it."
        } else {
            "This app needs access to Bluetooth Scan permission so that it " +
                    "can find the Score Counter."
        }
    }
}

class BluetoothConnectPermissionTextProvider: PermissionTextProvider {
    override fun getDescription(isPermanentlyDeclined: Boolean): String {
        return if(isPermanentlyDeclined) {
            "It seems you permanently declined Bluetooth Connect permission. " +
                    "You can go to the app settings to grant it."
        } else {
            "This app needs access to Bluetooth Connect permission so that it " +
                    "can connect to the Score Counter."
        }
    }
}

class BluetoothPermissionsTextProvider: PermissionTextProvider {
    override fun getDescription(isPermanentlyDeclined: Boolean): String {
        return if(isPermanentlyDeclined) {
            "It seems you permanently declined Bluetooth permissions. " +
                    "Please, navigate to App Settings and manually grant Bluetooth " +
                    "permissions to allow connection to BLE Score Counter."
        } else {
            "This app needs access to Bluetooth permissions so that it " +
                    "can connect to the Score Counter."
        }
    }
}