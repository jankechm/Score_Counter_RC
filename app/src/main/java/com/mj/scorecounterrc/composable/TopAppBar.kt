package com.mj.scorecounterrc.composable

import android.Manifest
import android.annotation.SuppressLint
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import androidx.core.content.ContextCompat.startForegroundService
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mj.scorecounterrc.R
import com.mj.scorecounterrc.data.model.DeviceCard
import com.mj.scorecounterrc.service.RcService
import com.mj.scorecounterrc.ui.theme.ScoreCounterRCTheme
import com.mj.scorecounterrc.ui.theme.TopAppBarContainerClr
import com.mj.scorecounterrc.util.findActivity
import com.mj.scorecounterrc.util.hasBtPermissions
import com.mj.scorecounterrc.util.openAppSettings
import com.mj.scorecounterrc.util.isBleSupported
import com.mj.scorecounterrc.viewmodel.ConnectionViewModel
import com.mj.scorecounterrc.viewmodel.ConnectionViewModel.ConnectionViewModelEvent
import com.mj.scorecounterrc.viewmodel.ConnectionViewModel.ConnectionState
import com.mj.scorecounterrc.viewmodel.EnableRequestSharedViewModel
import com.mj.scorecounterrc.viewmodel.EnableRequestSharedViewModel.EnableRequestedEvent
import android.content.Intent


@Composable
fun ScRcTopAppBarRoot() {
    val connectionViewModel = hiltViewModel<ConnectionViewModel>()
    val enableRequestSharedViewModel = hiltViewModel<EnableRequestSharedViewModel>()

    val onConnectionViewModelEvent = connectionViewModel::onEvent
    val connectionState = connectionViewModel.connectionState.collectAsStateWithLifecycle()
    val isScanning = connectionViewModel.isScanning.collectAsStateWithLifecycle()
    val bleDeviceCards = connectionViewModel.bleDeviceCards.collectAsStateWithLifecycle()
    val bluetoothEnableRequest = connectionViewModel.bluetoothEnableRequest
        .collectAsStateWithLifecycle()
    val onEnableRequestEvent = enableRequestSharedViewModel::onEvent

    ScRcTopAppBar(
        onConnectionViewModelEvent = onConnectionViewModelEvent,
        onEnableRequestEvent = onEnableRequestEvent,
        connectionState,
        isScanning,
        bleDeviceCards,
        bluetoothEnableRequest
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScRcTopAppBar(
    onConnectionViewModelEvent: (ConnectionViewModelEvent) -> Unit,
    onEnableRequestEvent: (EnableRequestedEvent) -> Unit,
    connectionState: State<ConnectionState>,
    isScanning: State<Boolean>,
    bleDeviceCards: State<List<DeviceCard>>,
    bluetoothEnableRequest: State<Boolean>
) {
    val btPermissions = arrayOf(
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_SCAN
    )

    val context = LocalContext.current
    val activity = context.findActivity()

    var showConnectionDialog by rememberSaveable { mutableStateOf(false) }
    var showRequestBtPermissionsRationale by rememberSaveable { mutableStateOf(false) }
    var showRequestNotificationPermissionRationale by rememberSaveable { mutableStateOf(false) }
    var showBtAppSettingsDialog by rememberSaveable { mutableStateOf(false) }

    val bluetoothPermissionResultLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { result ->
            val containsPermanentDenial = result.any {
                !it.value && !shouldShowRequestPermissionRationale(activity, it.key)
            }
            val containsDenial = result.values.any { !it }
            val allGranted = result.values.all { it }

            when {
                containsPermanentDenial -> {
                    showBtAppSettingsDialog = true
                }
                containsDenial -> {
                    showRequestBtPermissionsRationale = true
                }
                allGranted -> {
                    showConnectionDialog = true
                }
            }
        }
    )

    val notificationPermissionResultLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { result ->
            if (!result && shouldShowRequestPermissionRationale(
                    activity, Manifest.permission.POST_NOTIFICATIONS))
            {
                showRequestNotificationPermissionRationale = true
            }
        }
    )

    TopAppBar(
        title = { Text(text = "Score Counter RC") },
        actions = {
            val bluetoothIconResourceId = when(connectionState.value) {
                ConnectionState.CONNECTED -> R.drawable.bluetooth_connected
                ConnectionState.NOT_CONNECTED -> R.drawable.bluetooth
                ConnectionState.MANUALLY_DISCONNECTED -> R.drawable.bluetooth_disabled
            }

            IconButton(
                onClick = {
                    if (context.isBleSupported()) {
                        bluetoothPermissionResultLauncher.launch(btPermissions)
                        showConnectionDialog = context.hasBtPermissions()
                    } else {
                        Toast.makeText(context,
                            "Bluetooth Low Energy is not supported on this device!",
                            Toast.LENGTH_LONG).show()
                    }
                }) {
                    Icon(
                        painter = painterResource(id = bluetoothIconResourceId),
                        contentDescription = "Connection"
                    )
            }
            IconButton(onClick = { /*TODO*/ }) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = TopAppBarContainerClr
        )
    )

    if (showConnectionDialog) {
        if (bluetoothEnableRequest.value) {
            LaunchedEffect(true) {
                onEnableRequestEvent(EnableRequestedEvent.EnableBluetooth)
            }
        }

        // Show the ConnectionDialog only if there's no request to enable Bluetooth
        // = Bluetooth is already enabled.
        if (!bluetoothEnableRequest.value) {
            ConnectionDialog(
                onDismiss = {
                    showConnectionDialog = false
                    onConnectionViewModelEvent(ConnectionViewModelEvent.CloseConnectionDialog)
                },
                onEvent = onConnectionViewModelEvent,
                connectionState = connectionState,
                isScanning = isScanning,
                bleDeviceCards = bleDeviceCards
            )
        } else {
            // Try next time
            showConnectionDialog = false
        }
    }

    if (showRequestBtPermissionsRationale) {
        PermissionDialog(
            permissionTextProvider = BluetoothPermissionsTextProvider(),
            isPermanentlyDeclined = false,
            onDismiss = { showRequestBtPermissionsRationale = false },
            onConfirmBtnClick = {
                showRequestBtPermissionsRationale = false
                bluetoothPermissionResultLauncher.launch(btPermissions)
            }
        )
    }
    if (showBtAppSettingsDialog) {
        PermissionDialog(
            permissionTextProvider = BluetoothPermissionsTextProvider(),
            isPermanentlyDeclined = true,
            onDismiss = { showBtAppSettingsDialog = false },
            onConfirmBtnClick = {
                showBtAppSettingsDialog = false
                activity.openAppSettings()
            }
        )
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        connectionState.value == ConnectionState.CONNECTED) {
        LaunchedEffect(true) {
            notificationPermissionResultLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)

            val intent = Intent(context, RcService::class.java)
            startForegroundService(context, intent)
        }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            showRequestNotificationPermissionRationale) {
        PermissionDialog(
            permissionTextProvider = NotificationPermissionTextProvider(),
            isPermanentlyDeclined = false,
            onDismiss = { showRequestNotificationPermissionRationale = false },
            onConfirmBtnClick = {
                showRequestNotificationPermissionRationale = false
                notificationPermissionResultLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        )
    }
}

@SuppressLint("UnrememberedMutableState")
@Preview(showBackground = true)
@Composable
fun ConnectionDialogPreview() {
    ScoreCounterRCTheme {
        ConnectionDialog(
            onDismiss = {},
            onEvent = {},
            connectionState = mutableStateOf(ConnectionState.CONNECTED),
            isScanning = mutableStateOf(false),
            bleDeviceCards = mutableStateOf(listOf( // Pass mock DeviceCard list
                DeviceCard("Device 1", "12:34:56:78:90:AB"),
                DeviceCard("Device 2", "AB:CD:EF:12:34:56")
            ))
        )
    }
}

