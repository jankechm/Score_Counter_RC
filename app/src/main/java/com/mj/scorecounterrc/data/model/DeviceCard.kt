package com.mj.scorecounterrc.data.model

import android.bluetooth.BluetoothDevice

data class DeviceCard(
    var name: String,
    var address: String,
    var device: BluetoothDevice? = null,
)
