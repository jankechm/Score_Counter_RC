package com.mj.scorecounterrc.listener

import android.bluetooth.BluetoothDevice
import com.mj.scorecounterrc.broadcastreceiver.BtStateChangedReceiver

/** A listener containing callback methods to be registered with [BtStateChangedReceiver].*/
class BtBroadcastListener {
    var onBluetoothOff: (() -> Unit)? = null
    var onBluetoothOn: (() -> Unit)? = null
    var onBondStateChanged: ((Int, BluetoothDevice?) -> Unit)? = null
}
