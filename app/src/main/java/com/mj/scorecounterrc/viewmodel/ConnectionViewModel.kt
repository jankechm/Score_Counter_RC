package com.mj.scorecounterrc.viewmodel

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mj.scorecounterrc.scorecounter.BLEScanner
import com.mj.scorecounterrc.scorecounter.BLEScanner.BluetoothState
import com.mj.scorecounterrc.ble.ConnectionManager
import com.mj.scorecounterrc.data.model.DeviceCard
import com.mj.scorecounterrc.listener.ConnectionEventListener
import com.mj.scorecounterrc.scorecounter.ScoreCounterConnectionManager
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
    scoreCounterConnectionManager: ScoreCounterConnectionManager
) : ViewModel() {

    private val _isConnected = MutableStateFlow(
        scoreCounterConnectionManager.isBleScoreCounterConnected())
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _bluetoothEnableRequest = MutableStateFlow(false)
    val bluetoothEnableRequest: StateFlow<Boolean> = _bluetoothEnableRequest.asStateFlow()

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
            _isConnected.update { false }
        }
        onMtuChanged = { _,_ ->
            _isConnected.update { true }
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


    fun startScan() {
        if (!isScanning.value) {
            bleScanner.startBleScan()
            _isScanning.update { true }

        }
    }

    fun onEvent(event: ConnectionViewModelEvent) {
        when (event) {
            ConnectionViewModelEvent.ConnectionButtonClicked -> {
                TODO()
            }
            ConnectionViewModelEvent.SettingsButtonClicked -> {
                TODO()
            }

            is ConnectionViewModelEvent.Connect -> TODO()
            ConnectionViewModelEvent.Disconnect -> TODO()
            ConnectionViewModelEvent.StartScan -> TODO()
            ConnectionViewModelEvent.StopScan -> TODO()
            ConnectionViewModelEvent.ResetBluetoothEnableRequest -> {
                _bluetoothEnableRequest.value = false
            }
        }
    }

    sealed interface ConnectionViewModelEvent {
        data object ConnectionButtonClicked : ConnectionViewModelEvent
        data object SettingsButtonClicked : ConnectionViewModelEvent
        data object StartScan : ConnectionViewModelEvent
        data object StopScan : ConnectionViewModelEvent
        data class Connect(val device: BluetoothDevice) : ConnectionViewModelEvent
        data object Disconnect : ConnectionViewModelEvent
        data object ResetBluetoothEnableRequest : ConnectionViewModelEvent
    }
}