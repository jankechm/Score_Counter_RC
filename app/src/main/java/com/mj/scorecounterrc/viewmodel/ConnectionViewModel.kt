package com.mj.scorecounterrc.viewmodel

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mj.scorecounterrc.Constants
import com.mj.scorecounterrc.communication.scorecounter.BLEScanner
import com.mj.scorecounterrc.communication.scorecounter.BLEScanner.BluetoothState
import com.mj.scorecounterrc.ble.ConnectionManager
import com.mj.scorecounterrc.data.model.DeviceCard
import com.mj.scorecounterrc.listener.ConnectionEventListener
import com.mj.scorecounterrc.communication.scorecounter.ScoreCounterConnectionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConnectionViewModel @Inject constructor(
    private val bleScanner: BLEScanner,
    private val scoreCounterConnectionManager: ScoreCounterConnectionManager
) : ViewModel() {

    private val _connectionState = MutableStateFlow(
        if (scoreCounterConnectionManager.isBleScoreCounterConnected())
            ConnectionState.CONNECTED else if (scoreCounterConnectionManager.manuallyDisconnected)
                ConnectionState.MANUALLY_DISCONNECTED else ConnectionState.NOT_CONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _bluetoothEnableRequest = MutableStateFlow(false)
    val bluetoothEnableRequest: StateFlow<Boolean> = _bluetoothEnableRequest.asStateFlow()

    private val handler: Handler = Handler(Looper.getMainLooper())

    @SuppressLint("MissingPermission")
    val bleDeviceCards: StateFlow<List<DeviceCard>> = bleScanner.bleScanResults
        .map { scanResults ->
            scanResults.map { scanResult ->
                DeviceCard(
                    name = scanResult.device.name ?: "Unknown Device",
                    address = scanResult.device.address,
                    device = scanResult.device
                )
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),
            emptyList()
        )


    private val connectionEventListener = ConnectionEventListener().apply {
        onDisconnect = { _ ->
            _connectionState.update {
                if (scoreCounterConnectionManager.manuallyDisconnected)
                    ConnectionState.MANUALLY_DISCONNECTED else ConnectionState.NOT_CONNECTED }
        }
        onMtuChanged = { _,_ ->
            scoreCounterConnectionManager.manuallyDisconnected = false
            _connectionState.update { ConnectionState.CONNECTED }
        }
    }


    init {
        ConnectionManager.registerListener(connectionEventListener)

        viewModelScope.launch {
            bleScanner.bluetoothState.collect { state ->
                when (state) {
                    BluetoothState.Off -> {
                        // Notify the UI to request Bluetooth enable
                        _bluetoothEnableRequest.value = true
                    }
                    // ... handle other states
                    BluetoothState.On -> _bluetoothEnableRequest.value = false
                }
            }
        }
    }


    private fun startScan() {
        if (!isScanning.value) {
            handler.postDelayed({
                _isScanning.update { false }
                bleScanner.stopBleScan()
            }, Constants.SCAN_PERIOD)
        }

        val success = bleScanner.startBleScan()
        if (success) {
            _isScanning.update { true }
        }
    }

    private fun stopScan() {
        bleScanner.stopBleScan()
        _isScanning.update { false }
    }

    private fun resetBleDeviceCards() {
        bleScanner.resetScanResults()
    }

    fun onEvent(event: ConnectionViewModelEvent) {
        when (event) {
            ConnectionViewModelEvent.SettingsButtonClicked -> {
                TODO()
            }
            is ConnectionViewModelEvent.Connect -> {
                scoreCounterConnectionManager.connect(event.device)
                resetBleDeviceCards()
                stopScan()
            }
            ConnectionViewModelEvent.Disconnect -> {
                scoreCounterConnectionManager.manuallyDisconnected = true
                scoreCounterConnectionManager.disconnect()
                resetBleDeviceCards()
            }
            ConnectionViewModelEvent.StartScan -> startScan()
            ConnectionViewModelEvent.StopScan -> stopScan()
            ConnectionViewModelEvent.ResetBluetoothEnableRequest ->
                _bluetoothEnableRequest.value = false
            ConnectionViewModelEvent.CloseConnectionDialog -> {
                resetBleDeviceCards()
                stopScan()
            }
        }
    }

    sealed interface ConnectionViewModelEvent {
        data object SettingsButtonClicked : ConnectionViewModelEvent
        data object StartScan : ConnectionViewModelEvent
        data object StopScan : ConnectionViewModelEvent
        data class Connect(val device: BluetoothDevice) : ConnectionViewModelEvent
        data object Disconnect : ConnectionViewModelEvent
        data object ResetBluetoothEnableRequest : ConnectionViewModelEvent
        data object CloseConnectionDialog : ConnectionViewModelEvent
    }

    enum class ConnectionState {
        CONNECTED,
        NOT_CONNECTED,
        MANUALLY_DISCONNECTED,
    }
}