package com.mj.scorecounterrc.scorecounter

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application.RECEIVER_NOT_EXPORTED
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.mj.scorecounterrc.Constants
import com.mj.scorecounterrc.ScoreCounterRcApp
import com.mj.scorecounterrc.ScoreSync
import com.mj.scorecounterrc.Storage
import com.mj.scorecounterrc.ble.Connect
import com.mj.scorecounterrc.ble.ConnectionManager
import com.mj.scorecounterrc.ble.ConnectionManager.isConnected
import com.mj.scorecounterrc.broadcastreceiver.BtStateChangedReceiver
import com.mj.scorecounterrc.data.manager.AppCfgManager
import com.mj.scorecounterrc.data.model.Score
import com.mj.scorecounterrc.listener.BtBroadcastListener
import com.mj.scorecounterrc.listener.ConnectionEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object ScoreCounterConnectionManager {
    var btAdapter: BluetoothAdapter? = null

    private var bleScoreCounter: BluetoothDevice? = null
    private var writableDisplayChar: BluetoothGattCharacteristic? = null

    private var manuallyDisconnected = false
    private var shouldTryConnect = false
    private var isSomeConnectionCoroutineRunning = false

    private var msgBuffer: String = ""

    private val handler: Handler = Handler(Looper.getMainLooper())

    private val applicationScope = CoroutineScope(SupervisorJob())

    private val btStateChangedReceiver = BtStateChangedReceiver()

    // Should be injected at app.onCreate()
    var app: ScoreCounterRcApp? = null


    enum class ReconnectionType {
        PERSISTED_DEVICE,
        LAST_DEVICE
    }


    private val connectionEventListener by lazy {
        ConnectionEventListener().apply {
            onMtuChanged = { btDevice, _ ->
                bleScoreCounter = btDevice
                writableDisplayChar = ConnectionManager.findCharacteristic(
                    btDevice, Constants.DISPLAY_WRITABLE_CHARACTERISTIC_UUID
                )

                handleBonding(btDevice)

                writableDisplayChar?.let {
                    ConnectionManager.enableNotifications(btDevice, it)
                    sendDayTimeToScoreCounter(btDevice, it)
                }

                if (app != null) {
                    Storage.saveDeviceAddress(btDevice.address)

                    handler.post {
                        Toast.makeText(
                            app!!,
                            "Connected to ${btDevice.address}", Toast.LENGTH_SHORT).show()
                    }
                }

                manuallyDisconnected = false
                shouldTryConnect = false

                // Trigger new sync
                ScoreSync.trySync()

                // TODO
//                val intent = Intent(this@BleScoreCounterApp, BleService::class.java)
//                startForegroundService(intent)

            }
            onNotificationsEnabled = { _,_ -> Timber.i( "Enabled notification") }
            onDisconnect = { bleDevice ->
                writableDisplayChar = null

                if (!manuallyDisconnected) {
                    startReconnectionCoroutine()
                }
//                else {
//                    val intent = Intent(this@BleScoreCounterApp, BleService::class.java)
//                    stopService(intent)
//                }

                app?.let {
                    handler.post {
                        Toast.makeText(
                            app!!,
                            "Disconnected from ${bleDevice.address}", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
            onCharacteristicChanged = { _, _, value ->
                val decoded = value.toString(Charsets.US_ASCII)
                msgBuffer += decoded

                val msgBufferLen = msgBuffer.length
                if (msgBufferLen >= 2 && msgBuffer[msgBufferLen-2] == '\r' &&
                    msgBuffer[msgBufferLen-1] == '\n') {
                    // Full message received, process it
                    val msg = msgBuffer.removeSuffix(Constants.CRLF)
                    Timber.d("Full message: $msg")

                    // A response from Score Counter to GET_SCORE (sync process)
                    if (msg.startsWith(Constants.SCORE_CMD_PREFIX)) {
                        val scoreAndTimestamp = msg.removePrefix(Constants.SCORE_CMD_PREFIX)
                        val scoreAndTimestampLst = scoreAndTimestamp.split('T')
                        if (scoreAndTimestampLst.size == 2) {
                            val scoreLst = scoreAndTimestampLst[0].split(':')
                            if (scoreLst.size == 2) {
                                try {
                                    val score1 = scoreLst[0].toInt()
                                    val score2 = scoreLst[1].toInt()
                                    val timestamp = scoreAndTimestampLst[1].toLong()

                                    ScoreSync.setScoreCounterData(Score(score1, score2), timestamp)
                                } catch (ex: NumberFormatException) {
                                    Timber.e("Problem parsing " +
                                            Constants.SCORE_CMD_PREFIX + " command.", ex)
                                }
                            }
                        }
                    }
                    // Reset the buffer
                    msgBuffer = ""
                }
            }
        }
    }

    private val btBroadcastListener by lazy {
        BtBroadcastListener().apply {
            onBluetoothOff = {
                ConnectionManager.disconnectAllDevices()
            }
            onBluetoothOn = {
                if (!manuallyDisconnected) {
                    if (bleScoreCounter != null) {
                        startReconnectionCoroutine()
                    } else {
                        startConnectionToPersistedDeviceCoroutine()
                    }
                }
            }
            onBondStateChanged = { bondState, bleDevice ->
                if (bondState == BluetoothDevice.BOND_BONDED && bleDevice != null &&
                    !bleDevice.isConnected() &&
                    ConnectionManager.pendingOperation !is Connect
                ) {
                    app?.let { app ->
                        ConnectionManager.connect(bleDevice, app)
                    }
                }
            }
        }
    }


    fun registerReceivers() {
        val btStateChangedFilter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            app?.registerReceiver(btStateChangedReceiver, btStateChangedFilter, RECEIVER_NOT_EXPORTED)
        } else {
            app?.registerReceiver(btStateChangedReceiver, btStateChangedFilter)
        }
    }

    fun registerListeners() {
        ConnectionManager.registerListener(connectionEventListener)
        btStateChangedReceiver.registerListener(btBroadcastListener)
    }

    @SuppressLint("MissingPermission")
    fun startConnectionToPersistedDeviceCoroutine() {
        if (isSomeConnectionCoroutineRunning || app == null) {
            Timber.i( "Some connection coroutine already running!")
            return
        }

        val savedDeviceAddress = Storage.getSavedDeviceAddress()

        if (btAdapter != null && savedDeviceAddress != null) {
            val savedDevice: BluetoothDevice? =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    btAdapter!!.getRemoteLeDevice(savedDeviceAddress, BluetoothDevice.ADDRESS_TYPE_PUBLIC)
                } else {
                    btAdapter!!.getRemoteDevice(savedDeviceAddress)
                }

            if (savedDevice != null) {
                if (ActivityCompat.checkSelfPermission(
                        app!!, Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED &&
                    savedDevice.bondState == BluetoothDevice.BOND_BONDED) {
                    applicationScope.launch(Dispatchers.IO) {
                        tryConnect(savedDevice, ReconnectionType.PERSISTED_DEVICE)
                    }
                } else {
                    Timber.i( "Last BLE device was not bonded, auto-connection canceled!")
                }
            }
        }

        isSomeConnectionCoroutineRunning = false
    }

    fun startReconnectionCoroutine() {
        if (bleScoreCounter == null) {
            Timber.i( "BluetoothDevice is null!")
            return
        }

        applicationScope.launch(Dispatchers.IO) {
            tryConnect(bleScoreCounter!!, ReconnectionType.LAST_DEVICE)
        }
    }

    private suspend fun tryConnect(bleDevice: BluetoothDevice, reconnectionType: ReconnectionType) {
        if (isSomeConnectionCoroutineRunning || app == null) {
            Timber.i( "Some connection coroutine already running!")
            return
        }

        ConnectionManager.disconnectAllDevices()

        isSomeConnectionCoroutineRunning = true

        val maxImmediateRetries = 3
        val initialDelayMillis = 100L
        val connectionDelayMillis = 2_000L
        val retryDelayMillis = 24_000L

        var connectionAttempts = 0

        shouldTryConnect = true

        delay(initialDelayMillis)
        while (btAdapter != null && btAdapter!!.isEnabled && app!!.hasBtPermissions() &&
            shouldTryConnect
        ) {
            if (ConnectionManager.pendingOperation !is Connect) {
                ConnectionManager.connect(bleDevice, app!!.applicationContext)
            }
            connectionAttempts++
            delay(connectionDelayMillis)

            if (bleDevice.isConnected()) {
                if (reconnectionType == ReconnectionType.PERSISTED_DEVICE) {
                    Timber.i( "Auto-connection to persisted device " +
                            "${bleDevice.address} successful!")
                } else {
                    Timber.i( "Reconnected to last device ${bleDevice.address}!")
                }
                break
            }

            if (connectionAttempts % maxImmediateRetries == 0) {
                delay(retryDelayMillis)
            }
        }

        isSomeConnectionCoroutineRunning = false
    }

    private fun sendDayTimeToScoreCounter(btDevice: BluetoothDevice,
                                          characteristic: BluetoothGattCharacteristic) {
        val currDateTime = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("e d.M.yy H:m:s")

        Timber.i(Constants.SET_TIME_CMD_PREFIX + currDateTime.format(formatter))
        ConnectionManager.writeCharacteristic(
            btDevice, characteristic,
            (Constants.SET_TIME_CMD_PREFIX + currDateTime.format(formatter) +
                    Constants.CRLF).
            toByteArray(Charsets.US_ASCII)
        )
    }

    fun sendScoreToScoreCounter(score: Score, timestamp: Long): Boolean {
        var isSent = false

        if (isBleScoreCounterConnected()) {
            if (writableDisplayChar != null) {
                val updateScoreCmd = Constants.SET_SCORE_CMD_PREFIX +
                        "${score.left}:${score.right}T$timestamp${Constants.CRLF}"

                Timber.d("Sending BLE message: $updateScoreCmd")

                ConnectionManager.writeCharacteristic(
                    bleScoreCounter!!, writableDisplayChar!!,
                    updateScoreCmd.toByteArray(Charsets.US_ASCII)
                )

                isSent = true
            } else {
                Timber.d("Display connected, but characteristic is null!")
            }
        } else {
            Timber.d("Display not connected.")
        }

        return isSent
    }

    fun sendSyncRequestToScoreCounter() {
        if (isBleScoreCounterConnected()) {
            if (writableDisplayChar != null) {
                val getScoreCmd = "${Constants.GET_SCORE_CMD}${Constants.CRLF}"
                Timber.d("Sending BLE message: $getScoreCmd")
                ConnectionManager.writeCharacteristic(
                    bleScoreCounter!!, writableDisplayChar!!,
                    getScoreCmd.toByteArray(Charsets.US_ASCII)
                )
            } else {
                Timber.d("Display connected, but characteristic is null!")
            }
        } else {
            Timber.d("Display not connected.")
        }
    }

    @SuppressLint("MissingPermission")
    fun handleBonding(btDevice: BluetoothDevice) {
        if (AppCfgManager.appCfg.askToBond) {
            btDevice.createBond()
        }
    }

    fun isBleScoreCounterConnected(): Boolean {
        return bleScoreCounter != null && bleScoreCounter!!.isConnected()
    }
}