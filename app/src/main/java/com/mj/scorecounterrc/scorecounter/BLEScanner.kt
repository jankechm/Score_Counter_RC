package com.mj.scorecounterrc.scorecounter

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.widget.Toast
import com.mj.scorecounterrc.broadcastreceiver.BtStateChangedReceiver
import com.mj.scorecounterrc.listener.BtBroadcastListener
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject

class BLEScanner @Inject constructor(@ApplicationContext private val context: Context) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE)
            as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    private val _bleScanResults = MutableStateFlow<List<ScanResult>>(emptyList())
    val bleScanResults: StateFlow<List<ScanResult>> = _bleScanResults.asStateFlow()

    private val _bluetoothState = MutableStateFlow(
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled) BluetoothState.On
        else BluetoothState.Off
    )
    val bluetoothState: StateFlow<BluetoothState> = _bluetoothState.asStateFlow()


    private val btBroadcastListener by lazy {
        BtBroadcastListener().apply {
            onBluetoothOn = {
                _bluetoothState.value = BluetoothState.On
            }
            onBluetoothOff = {
                _bluetoothState.value = BluetoothState.Off
            }
        }
    }

    private val scanFilter = ScanFilter.Builder()
        // TODO: Set device name
//        .setDeviceName(Constants.BLE_DISPLAY_NAME)
        .build()

    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
//        .setCallbackType(ScanSettings.CALLBACK_TYPE_FIRST_MATCH)
        .build()

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)

            addScanResult(result)
        }

        override fun onScanFailed(errorCode: Int) {
            Timber.e("onScanFailed: code $errorCode")
        }
    }


    sealed interface BluetoothState {
        data object On : BluetoothState
        data object Off : BluetoothState
    }


    init {
        BtStateChangedReceiver.registerListener(btBroadcastListener)
    }


    /**
     * It is assumed that the required permissions are already granted and bluetooth is enabled
     * before calling this method.
     */
    @SuppressLint("MissingPermission", "NotifyDataSetChanged")
    fun startBleScan(): Boolean {
        if (bluetoothAdapter != null) {
            if (bluetoothAdapter.isEnabled) {
                _bluetoothState.value = BluetoothState.On
                bluetoothAdapter.bluetoothLeScanner.startScan(
                    listOf(scanFilter), scanSettings, scanCallback
                )
                return true
            } else {
                Timber.i("startBleScan(): Bluetooth is not enabled!")
                context.let {
                    Toast.makeText(it, "Can't start Bluetooth scan! Is Bluetooth on?",
                        Toast.LENGTH_LONG).show()
                }
                _bluetoothState.value = BluetoothState.Off
            }
        }
        else {
            Timber.w("startBleScan(): BluetoothAdapter is null!")
        }

        return false
    }

    /**
     * It is assumed that the required permissions are already granted and bluetooth is enabled
     * before calling this method.
     */
    @SuppressLint("MissingPermission")
    fun stopBleScan() {
        if (bluetoothAdapter != null) {
            bluetoothAdapter.bluetoothLeScanner.stopScan(scanCallback)
        }
        else {
            Timber.i("stopBleScan(): BluetoothAdapter is null!")
        }
    }

    @SuppressLint("MissingPermission")
    private fun addScanResult(scanResult: ScanResult) {
        val updatedList = _bleScanResults.value.toMutableList()

        if (updatedList.none { it.device.address == scanResult.device.address }) {
            with(scanResult.device) {
                var msg = "Found BLE device! Name: ${name ?: "Unnamed"}, address: $address"
                // TODO Maybe change uuids to result.scanRecord.serviceUuids
                uuids?.let {
                    msg += ", UUIDS:"
                    it.forEachIndexed { i, parcelUuid ->
                        msg += "\n${i+1}: ${parcelUuid.uuid}"
                    }
                }
                Timber.i(msg)
            }
            updatedList.add(scanResult)
            _bleScanResults.value = updatedList
        }
    }

    fun resetScanResults() {
        _bleScanResults.value = emptyList()
    }
}