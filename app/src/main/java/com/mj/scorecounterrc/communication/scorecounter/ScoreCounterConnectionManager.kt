package com.mj.scorecounterrc.communication.scorecounter

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.mj.scorecounterrc.Constants
import com.mj.scorecounterrc.ScoreSync
import com.mj.scorecounterrc.ble.Connect
import com.mj.scorecounterrc.ble.ConnectionManager
import com.mj.scorecounterrc.ble.ConnectionManager.isConnected
import com.mj.scorecounterrc.broadcastreceiver.BtStateChangedReceiver
import com.mj.scorecounterrc.communication.scorecounter.listener.SCCMListener
import com.mj.scorecounterrc.data.manager.StorageManager
import com.mj.scorecounterrc.data.model.Score
import com.mj.scorecounterrc.data.model.ScoreCounterCfg
import com.mj.scorecounterrc.listener.BtBroadcastListener
import com.mj.scorecounterrc.listener.ConnectionEventListener
import com.mj.scorecounterrc.util.hasBtPermissions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.lang.ref.WeakReference
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class ScoreCounterConnectionManager @Inject constructor(
    @ApplicationContext private var context: Context,
    private val scoreSync: Provider<ScoreSync>,
    private val storageManager: StorageManager,
) : ScoreCounterMessageSender {

    private val btAdapter: BluetoothAdapter? = context.getSystemService(BluetoothManager::class.java)
        ?.adapter

    private var bleScoreCounter: BluetoothDevice? = null
    private var writableDisplayChar: BluetoothGattCharacteristic? = null

    var manuallyDisconnected = false
    private var shouldTryConnect = false
    private val isSomeConnectionCoroutineRunning = AtomicBoolean(false)

    private var msgBuffer: String = ""

    private val handler: Handler = Handler(Looper.getMainLooper())

    private val applicationScope = CoroutineScope(SupervisorJob())

    private var listeners: MutableSet<WeakReference<SCCMListener>> = ConcurrentHashMap.newKeySet()


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
                    sendDayTimeToScoreCounter()
                }

                storageManager.saveDeviceAddress(btDevice.address)

                handler.post {
                    Toast.makeText(
                        context,
                        "Connected to ${btDevice.address}", Toast.LENGTH_SHORT).show()
                }

                manuallyDisconnected = false
                shouldTryConnect = false

                // Trigger new sync
                scoreSync.get().trySync()

                listeners.forEach { it.get()?.onMtuChanged?.invoke() }
            }
            onNotificationsEnabled = { _,_ -> Timber.i( "Enabled notification") }
            onDisconnect = { bleDevice ->
                writableDisplayChar = null

                if (!manuallyDisconnected) {
                    startReconnectionCoroutine()
                }

                listeners.forEach { it.get()?.onDisconnect?.invoke() }

                context.let {
                    handler.post {
                        Toast.makeText(
                            context,
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

                                    scoreSync.get().setScoreCounterData(Score(score1, score2), timestamp)
                                } catch (ex: NumberFormatException) {
                                    Timber.e("Problem parsing " +
                                            Constants.SCORE_CMD_PREFIX + " command.", ex)
                                }
                            }
                        }
                    } else if (msg.startsWith(Constants.CONFIG_CMD_PREFIX)) {
                        val jsonStr = msg.removePrefix(Constants.CONFIG_CMD_PREFIX)
                        try {
                            val scoreCounterCfg = Json.decodeFromString<ScoreCounterCfg>(jsonStr)
                            listeners.forEach { it.get()?.onCfgReceived?.invoke(scoreCounterCfg) }
                        } catch (ex: Exception) {
                            when (ex) {
                                is SerializationException,
                                is IllegalArgumentException -> {
                                    Timber.e("Problem decoding JSON from " +
                                            Constants.CONFIG_CMD_PREFIX, ex
                                    )
                                }
                                else -> {
                                    Timber.e("Problem with " +
                                            Constants.CONFIG_CMD_PREFIX, ex
                                    )
                                }
                            }
                        }
                    } else if (msg.startsWith(Constants.CFG_PERSIST_ACK_CMD)) {
                        listeners.forEach { it.get()?.onSentCfgAck?.invoke() }
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
                    context.let { app ->
                        ConnectionManager.connect(bleDevice, app)
                    }
                }
            }
        }
    }


    init {
        registerListeners()
    }


    private fun registerListeners() {
        ConnectionManager.registerListener(connectionEventListener)
        BtStateChangedReceiver.registerListener(btBroadcastListener)
    }

    @SuppressLint("MissingPermission")
    fun startConnectionToPersistedDeviceCoroutine() {
        val savedDeviceAddress = storageManager.getSavedDeviceAddress()

        if (btAdapter != null && savedDeviceAddress != null) {
            val savedDevice: BluetoothDevice? =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    btAdapter.getRemoteLeDevice(savedDeviceAddress, BluetoothDevice.ADDRESS_TYPE_PUBLIC)
                } else {
                    btAdapter.getRemoteDevice(savedDeviceAddress)
                }

            if (savedDevice != null) {
                if (ActivityCompat.checkSelfPermission(
                        context, Manifest.permission.BLUETOOTH_CONNECT
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
    }

    private fun startReconnectionCoroutine() {
        if (bleScoreCounter == null) {
            Timber.i( "BluetoothDevice is null!")
            return
        }

        applicationScope.launch(Dispatchers.IO) {
            tryConnect(bleScoreCounter!!, ReconnectionType.LAST_DEVICE)
        }
    }

    private suspend fun tryConnect(bleDevice: BluetoothDevice, reconnectionType: ReconnectionType) {
        if (isSomeConnectionCoroutineRunning.get()) {
            Timber.i( "Some connection coroutine already running!")
            return
        }

        isSomeConnectionCoroutineRunning.set(true)

        ConnectionManager.disconnectAllDevices()

        val maxImmediateRetries = 3
        val initialDelayMillis = 100L
        val connectionDelayMillis = 2_000L
        val retryDelayMillis = 24_000L

        var connectionAttempts = 0

        shouldTryConnect = true

        delay(initialDelayMillis)
        while (btAdapter != null && btAdapter.isEnabled && context.hasBtPermissions() &&
            shouldTryConnect
        ) {
            if (ConnectionManager.pendingOperation !is Connect) {
                ConnectionManager.connect(bleDevice, context.applicationContext)
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

        isSomeConnectionCoroutineRunning.set(false)
    }

    private fun sendMsgToScoreCounter(message: String): Boolean {
        var isSent = false

        if (isBleScoreCounterConnected()) {
            if (writableDisplayChar != null) {
                Timber.d("Sending BLE message: $message")

                ConnectionManager.writeCharacteristic(
                    bleScoreCounter!!,
                    writableDisplayChar!!,
                    message.toByteArray(Charsets.US_ASCII)
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

    private fun sendDayTimeToScoreCounter(): Boolean {
        val currDateTime = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("e d.M.yy H:m:s")
        val message = Constants.SET_TIME_CMD_PREFIX + currDateTime.format(formatter) +
                Constants.CRLF

        Timber.i(Constants.SET_TIME_CMD_PREFIX + currDateTime.format(formatter))

        return sendMsgToScoreCounter(message)
    }

    fun sendScoreToScoreCounter(score: Score, timestamp: Long): Boolean {
        val message = Constants.SET_SCORE_CMD_PREFIX +
                "${score.left}:${score.right}T$timestamp${Constants.CRLF}"

        return sendMsgToScoreCounter(message)
    }

    fun sendGetConfigRequest(): Boolean {
        val message = "${Constants.GET_CONFIG_CMD}${Constants.CRLF}"
        return sendMsgToScoreCounter(message)
    }

    fun sendSyncRequestToScoreCounter(): Boolean {
        val message = "${Constants.GET_SCORE_CMD}${Constants.CRLF}"
        return sendMsgToScoreCounter(message)
    }

    fun sendShowScoreSetting(showScore: Boolean): Boolean {
        val showScoreVal = if (showScore) 1 else 0
        val message = "${Constants.SET_SHOW_SCORE_CMD_PREFIX}$showScoreVal${Constants.CRLF}"
        return sendMsgToScoreCounter(message)
    }

    fun sendShowTimeSetting(showTime: Boolean): Boolean {
        val showTimeVal = if (showTime) 1 else 0
        val message = "${Constants.SET_SHOW_TIME_CMD_PREFIX}$showTimeVal${Constants.CRLF}"
        return sendMsgToScoreCounter(message)
    }

    fun sendScrollSetting(scroll: Boolean): Boolean {
        val scrollVal = if (scroll) 1 else 0
        val message = "${Constants.SET_SCROLL_CMD_PREFIX}$scrollVal${Constants.CRLF}"
        return sendMsgToScoreCounter(message)
    }

    fun sendBrightnessSetting(brightness: Int): Boolean {
        val message = "${Constants.SET_BRIGHTNESS_CMD_PREFIX}$brightness${Constants.CRLF}"
        return sendMsgToScoreCounter(message)
    }

    fun sendPersistConfig(config: ScoreCounterCfg): Boolean {
        val json = Json { encodeDefaults = true }
        val jsonStr = json.encodeToString(config)

        val message = "${Constants.PERSIST_CONFIG_CMD_PREFIX}$jsonStr${Constants.CRLF}"
        return sendMsgToScoreCounter(message)
    }

    @SuppressLint("MissingPermission")
    fun handleBonding(btDevice: BluetoothDevice) {
        btDevice.createBond()
    }

    fun isBleScoreCounterConnected(): Boolean {
        return bleScoreCounter != null && bleScoreCounter!!.isConnected()
    }

    override fun sendScore(score: Score, timestamp: Long) {
        sendScoreToScoreCounter(score, timestamp)
    }

    override fun requestScoreSync() {
        sendSyncRequestToScoreCounter()
    }

    fun connect(device: BluetoothDevice) {
        ConnectionManager.connect(device, context)
    }

    fun disconnect() {
        manuallyDisconnected = true
        ConnectionManager.disconnectAllDevices()
    }

    @Synchronized
    fun registerListener(listener: SCCMListener) {
        if (listeners.map { it.get() }.none { it?.equals(listener) == true }) {
            listeners.add(WeakReference(listener))
            listeners.removeIf { it.get() == null }

            Timber.d("Added a Score Counter Connection Manager listener, " +
                    "${listeners.size} listeners total")
        }
    }

    @Synchronized
    fun unregisterListener(listener: SCCMListener) {
        listeners.removeIf { it.get() == listener || it.get() == null }
        Timber.d("Removed a Score Counter Connection Manager listener, " +
                "${listeners.size} listeners total")
    }
}