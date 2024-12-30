package com.mj.scorecounterrc.composable

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mj.scorecounterrc.data.model.DeviceCard
import com.mj.scorecounterrc.ui.theme.ConnectButtonContainerClr
import com.mj.scorecounterrc.ui.theme.DisconnectButtonContainerClr
import com.mj.scorecounterrc.ui.theme.ScScanResultContainerClr
import com.mj.scorecounterrc.ui.theme.ScanButtonContainerClr
import com.mj.scorecounterrc.viewmodel.ConnectionViewModel

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionDialog(
    onDismiss: () -> Unit,
    onEvent: (ConnectionViewModel.ConnectionViewModelEvent) -> Unit,
    connectionState: State<ConnectionViewModel.ConnectionState>,
    isScanning: State<Boolean>,
    bleDeviceCards: State<List<DeviceCard>>
) {
    var selectedBleDevice by rememberSaveable { mutableStateOf<BluetoothDevice?>(null) }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .background(Color.White, shape = RoundedCornerShape(16.dp))
            .width(320.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Button(
                onClick = {
                    if (isScanning.value) {
                        onEvent(ConnectionViewModel.ConnectionViewModelEvent.StopScan)
                    } else {
                        onEvent(ConnectionViewModel.ConnectionViewModelEvent.StartScan)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ScanButtonContainerClr,
                    contentColor = Color.White,
                ),
            ) {
                Text(text = if (isScanning.value) "Stop Scan" else "Start Scan")
            }

            Spacer(modifier = Modifier.size(27.dp))

            Row {
                Button(
                    modifier = Modifier.alpha(
                        if (connectionState.value == ConnectionViewModel.ConnectionState.CONNECTED)
                            1f else 0f
                    ),
                    onClick = {
                        if (connectionState.value == ConnectionViewModel.ConnectionState.CONNECTED) {
                            onEvent(ConnectionViewModel.ConnectionViewModelEvent.Disconnect)
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DisconnectButtonContainerClr,
                        contentColor = Color.White,
                    ),
                ) {
                    Text(text = "Disconnect")
                }

                Spacer(modifier = Modifier.size(22.dp))

                Button(
                    modifier = Modifier
                        .alpha(if (selectedBleDevice != null) 1f else 0f),
                    onClick = {
                        if (selectedBleDevice != null) {
                            onEvent(
                                ConnectionViewModel.ConnectionViewModelEvent.Connect(
                                    selectedBleDevice!!
                                )
                            )
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ConnectButtonContainerClr,
                        contentColor = Color.White,
                    ),
                ) {
                    Text(text = "Connect")
                }
            }

            Spacer(modifier = Modifier.size(20.dp))

            Box(
                modifier = Modifier
                    .size(290.dp, 260.dp)
                    .border(border = BorderStroke(2.dp, Color.Black)),
                contentAlignment = Alignment.TopCenter,
            ) {
                LazyColumn(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .height(200.dp)
                ) {
                    itemsIndexed(bleDeviceCards.value) { index, bleDeviceCard ->
                        var bgColor = ScScanResultContainerClr
                        var textColor = Color.Black

                        if (bleDeviceCard.device != null &&
                            selectedBleDevice == bleDeviceCard.device
                        ) {
                            bgColor = Color.Black
                            textColor = Color.White
                        }

                        Spacer(modifier = Modifier.size(10.dp))

                        Card(
                            modifier = Modifier
                                .wrapContentHeight()
                                .wrapContentWidth(),
                            onClick = { selectedBleDevice = bleDeviceCard.device },
                            shape = RoundedCornerShape(20),
                            colors = CardDefaults.cardColors(
                                containerColor = bgColor
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(start = 9.dp, end = 9.dp, top = 4.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${index + 1}.",
                                    color = textColor
                                )
                                Spacer(modifier = Modifier.size(9.dp))
                                Column() {
                                    Text(
                                        text = "Name: " + bleDeviceCard.name,
                                        fontSize = 16.sp,
                                        color = textColor
                                    )
                                    Text(
                                        text = "MAC address: " + bleDeviceCard.address,
                                        fontSize = 16.sp,
                                        color = textColor
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.size(20.dp))
        }
    }
}