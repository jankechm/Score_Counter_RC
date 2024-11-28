package com.mj.scorecounterrc

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.getpebble.android.kit.PebbleKit
import com.getpebble.android.kit.util.PebbleDictionary
import com.mj.scorecounterrc.ble.Connect
import com.mj.scorecounterrc.ble.ConnectionManager
import com.mj.scorecounterrc.broadcastreceiver.BtStateChangedReceiver
import com.mj.scorecounterrc.broadcastreceiver.SCPebbleDataReceiver
import com.mj.scorecounterrc.data.manager.AppCfgManager
import com.mj.scorecounterrc.data.manager.ScoreManager
import com.mj.scorecounterrc.data.model.Score
import com.mj.scorecounterrc.listener.BtBroadcastListener
import com.mj.scorecounterrc.listener.ConnectionEventListener
import com.mj.scorecounterrc.listener.PebbleListener
import fr.bipi.treessence.file.FileLoggerTree
import com.mj.scorecounterrc.ble.ConnectionManager.isConnected
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.logging.FileHandler
import java.util.logging.Logger

class ScoreCounterRcApp : Application() {

    enum class ReconnectionType {
        PERSISTED_DEVICE,
        LAST_DEVICE
    }

    private val btAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothManager.adapter
    }

    private val scoreSync: ScoreSync by lazy {
        ScoreSync(this)
    }

    private val storage: Storage by lazy {
        Storage(this)
    }

    private val applicationScope = CoroutineScope(SupervisorJob())

    var bleDisplay: BluetoothDevice? = null
    var writableDisplayChar: BluetoothGattCharacteristic? = null

    var manuallyDisconnected = false
    var isShuttingDown = false
    var shouldTryConnect = false
    private var isSomeConnectionCoroutineRunning = false

    private val pebbleAppUUID = UUID.fromString(Constants.PEBBLE_APP_UUID)

    private val btStateChangedReceiver = BtStateChangedReceiver()
    private val scPebbleDataReceiver = SCPebbleDataReceiver(pebbleAppUUID)

    private val handler: Handler = Handler(Looper.getMainLooper())

    private var msgBuffer: String = ""


    private val connectionEventListener by lazy {
        ConnectionEventListener().apply {
            onMtuChanged = { btDevice, _ ->
                bleDisplay = btDevice
                writableDisplayChar = ConnectionManager.findCharacteristic(
                    btDevice, Constants.DISPLAY_WRITABLE_CHARACTERISTIC_UUID
                )

                handleBonding(btDevice)

                writableDisplayChar?.let {
                    ConnectionManager.enableNotifications(btDevice, it)
                    sendDayTime(btDevice, it)
                }

                storage.saveDeviceAddress(btDevice.address)

                handler.post {
                    Toast.makeText(applicationContext,
                        "Connected to ${btDevice.address}", Toast.LENGTH_SHORT).show()
                }

                manuallyDisconnected = false
                shouldTryConnect = false

                // Trigger new sync
                scoreSync.trySync()

                // TODO
//                val intent = Intent(this@BleScoreCounterApp, BleService::class.java)
//                startForegroundService(intent)

            }
            onNotificationsEnabled = { _,_ -> Timber.i( "Enabled notification") }
            onDisconnect = { bleDevice ->
                writableDisplayChar = null

                if (!manuallyDisconnected && !isShuttingDown) {
                    startReconnectionCoroutine()
                }
//                else {
//                    val intent = Intent(this@BleScoreCounterApp, BleService::class.java)
//                    stopService(intent)
//                }

                handler.post {
                    Toast.makeText(this@ScoreCounterRcApp,
                        "Disconnected from ${bleDevice.address}", Toast.LENGTH_SHORT).show()
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

                                    scoreSync.setScoreCounterData(Score(score1, score2), timestamp)
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
                    if (bleDisplay != null) {
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
                    ConnectionManager.connect(bleDevice, this@ScoreCounterRcApp)
                }
            }
        }
    }

    private val pebbleListener by lazy {
        PebbleListener().apply {
            onDataReceived = { pebbleDict ->
                handleReceivedPebbleData(pebbleDict)
            }
        }
    }

    class MyFileLoggerTree(logger: Logger, fileHandler: FileHandler?, path: String, nbFiles: Int,
                           priority: Int
    ) : FileLoggerTree(logger, fileHandler, path, nbFiles, priority
    ) {
        override fun createStackElementTag(element: StackTraceElement): String {
            return "(${element.fileName}:${element.lineNumber})#${element.methodName}"
        }
    }


    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(object : Timber.DebugTree() {
                override fun createStackElementTag(element: StackTraceElement): String {
                    return "(${element.fileName}:${element.lineNumber})#${element.methodName}"
                }
            })

//            val logsDir = getExternalFilesDir(null).toString() + File.separator + "logs"
//
//            val flt = FileLoggerTree.Builder()
//                .withFileName("score_counter_%g.log")
//                .withDirName(logsDir)
//                .withSizeLimit(5_242_880)
//                .withFileLimit(5)
//                .withMinPriority(Log.DEBUG)
//                .appendToFile(true)
//                .build();
//
//            Timber.plant(flt)
        }

        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        }

        ConnectionManager.registerListener(connectionEventListener)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            this.registerReceiver(btStateChangedReceiver, filter, RECEIVER_NOT_EXPORTED)
            this.registerReceiver(scPebbleDataReceiver,
                IntentFilter(com.getpebble.android.kit.Constants.INTENT_APP_RECEIVE),
                RECEIVER_EXPORTED
            )
        } else {
            this.registerReceiver(btStateChangedReceiver, filter)
            PebbleKit.registerReceivedDataHandler(applicationContext, scPebbleDataReceiver)
        }

        btStateChangedReceiver.registerListener(btBroadcastListener)
        scPebbleDataReceiver.registerListener(pebbleListener)

        PebbleKit.startAppOnPebble(this, UUID.fromString(Constants.PEBBLE_APP_UUID))
    }

    @SuppressLint("MissingPermission")
    fun startConnectionToPersistedDeviceCoroutine() {
        if (isSomeConnectionCoroutineRunning) {
            Timber.i( "Some connection coroutine already running!")
            return
        }

        val savedDeviceAddress = storage.getSavedDeviceAddress()

        if (btAdapter != null && savedDeviceAddress != null) {
            val savedDevice: BluetoothDevice? =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                btAdapter!!.getRemoteLeDevice(savedDeviceAddress, BluetoothDevice.ADDRESS_TYPE_PUBLIC)
            } else {
                btAdapter!!.getRemoteDevice(savedDeviceAddress)
            }

            if (savedDevice != null) {
                if (ActivityCompat.checkSelfPermission(
                        this, Manifest.permission.BLUETOOTH_CONNECT
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
        if (bleDisplay == null) {
            Timber.i( "BluetoothDevice is null!")
            return
        }

        applicationScope.launch(Dispatchers.IO) {
            tryConnect(bleDisplay!!, ReconnectionType.LAST_DEVICE)
        }
    }

    private suspend fun tryConnect(bleDevice: BluetoothDevice, reconnectionType: ReconnectionType) {
        if (isSomeConnectionCoroutineRunning) {
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
        while (btAdapter != null && btAdapter!!.isEnabled && hasBtPermissions() &&
                shouldTryConnect && !isShuttingDown) {
            if (ConnectionManager.pendingOperation !is Connect) {
                ConnectionManager.connect(bleDevice, this@ScoreCounterRcApp)
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

    fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(this, permission) ==
                PackageManager.PERMISSION_GRANTED

    fun hasBtPermissions(): Boolean =
        hasPermission(Manifest.permission.BLUETOOTH_SCAN)
                && hasPermission(Manifest.permission.BLUETOOTH_CONNECT)

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun hasNotificationsPermission(): Boolean =
        hasPermission(Manifest.permission.POST_NOTIFICATIONS)

    fun requestBtPermissions(activity: Activity) {
        ActivityCompat.requestPermissions(activity,
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            ),
            Constants.BT_PERMISSIONS_REQUEST_CODE
        )
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun requestNotificationsPermission(activity: Activity) {
        ActivityCompat.requestPermissions(activity,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            Constants.NOTIFICATIONS_PERMISSIONS_REQUEST_CODE)
    }


    @SuppressLint("MissingPermission")
    fun handleBonding(btDevice: BluetoothDevice) {
        if (AppCfgManager.appCfg.askToBond) {
            btDevice.createBond()
        }
    }

    /**
     * Send daytime to the BLE display
     */
    fun sendDayTime(btDevice: BluetoothDevice, characteristic: BluetoothGattCharacteristic) {
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

    fun sendScoreToScoreCounter(score: Score, timestamp: Long) {
        if (bleDisplay != null && bleDisplay!!.isConnected()) {
            if (writableDisplayChar != null) {
                val updateScoreCmd = Constants.SET_SCORE_CMD_PREFIX +
                        "${score.left}:${score.right}T$timestamp${Constants.CRLF}"

                Timber.d("Sending BLE message: $updateScoreCmd")

                ConnectionManager.writeCharacteristic(
                    bleDisplay!!, writableDisplayChar!!,
                    updateScoreCmd.toByteArray(Charsets.US_ASCII)
                )
            } else {
                Timber.d("Display connected, but characteristic is null!")
            }
        } else {
            Timber.d("Display not connected.")
        }
    }

    fun sendSyncRequestToScoreCounter() {
        if (bleDisplay != null && bleDisplay!!.isConnected()) {
            if (writableDisplayChar != null) {
                val getScoreCmd = "${Constants.GET_SCORE_CMD}${Constants.CRLF}"
                Timber.d("Sending BLE message: $getScoreCmd")
                ConnectionManager.writeCharacteristic(
                    bleDisplay!!, writableDisplayChar!!,
                    getScoreCmd.toByteArray(Charsets.US_ASCII)
                )
            } else {
                Timber.d("Display connected, but characteristic is null!")
            }
        } else {
            Timber.d("Display not connected.")
        }
    }

    private fun handleReceivedPebbleData(pebbleDict: PebbleDictionary?) {
        if (pebbleDict == null) {
            Timber.i("Empty PebbleDictionary received.")
        }
        else {
            pebbleDict.forEach { tuple -> Timber.d("Pebble tuple key: ${tuple.key} | " +
                    "value: ${tuple.value}") }
            pebbleDict.getUnsignedIntegerAsLong(Constants.FROM_PEBBLE_CMD_KEY)?.let { pebbleCmdKey ->
                when (pebbleCmdKey.toInt()) {
                    Constants.FROM_PEBBLE_CMD_SET_SCORE_VAL -> {
                        // Accept new score set by Pebble.
                        val score1 = pebbleDict.getUnsignedIntegerAsLong(
                            Constants.FROM_PEBBLE_SCORE_1_KEY)?.toInt()
                        val score2 = pebbleDict.getUnsignedIntegerAsLong(
                            Constants.FROM_PEBBLE_SCORE_2_KEY)?.toInt()
                        val timestamp = pebbleDict.getUnsignedIntegerAsLong(
                            Constants.FROM_PEBBLE_TIMESTAMP_KEY)
                        if (score1 != null && score2 != null && timestamp != null) {
                            sendScoreToScoreCounter(Score(score1, score2), timestamp)
                            ScoreManager.saveReceivedScore(Score(score1, score2), timestamp)
                        }
                    }
                    Constants.FROM_PEBBLE_CMD_SYNC_SCORE_VAL -> {
                        val score1 = pebbleDict.getUnsignedIntegerAsLong(
                            Constants.FROM_PEBBLE_SCORE_1_KEY)?.toInt()
                        val score2 = pebbleDict.getUnsignedIntegerAsLong(
                            Constants.FROM_PEBBLE_SCORE_2_KEY)?.toInt()
                        val timestamp = pebbleDict.getUnsignedIntegerAsLong(
                            Constants.FROM_PEBBLE_TIMESTAMP_KEY)
                        if (score1 != null && score2 != null && timestamp != null) {
                            scoreSync.setSmartwatchData(Score(score1, score2), timestamp)
                        }
                    }
                }
            }
        }
    }

    fun sendScoreToSmartwatch(score: Score, timestamp: Long) {
        sendScoreToPebble(score, timestamp)
    }

    fun sendScoreToPebble(score: Score, timestamp: Long) {
//        if (PebbleKit.isWatchConnected(this)) {
        val dict = PebbleDictionary()

        dict.addUint8(Constants.TO_PEBBLE_CMD_KEY, Constants.TO_PEBBLE_CMD_SET_SCORE_VAL.toByte())
        dict.addUint16(Constants.TO_PEBBLE_SCORE_1_KEY, score.left.toShort())
        dict.addUint16(Constants.TO_PEBBLE_SCORE_2_KEY, score.right.toShort())
        // The lower 32 bits should remain the same after timestamp.toInt() conversion.
        dict.addUint32(Constants.TO_PEBBLE_TIMESTAMP_KEY, timestamp.toInt())

        Timber.i("Sending score ${score.left}:${score.right} T=$timestamp to Pebble")

        PebbleKit.sendDataToPebble(this, pebbleAppUUID, dict)
//        }
    }

    fun sendSyncRequestToSmartwatch() {
        sendSyncRequestToPebble()
    }

    fun sendSyncRequestToPebble() {
        val dict = PebbleDictionary()

        dict.addUint8(Constants.TO_PEBBLE_CMD_KEY, Constants.TO_PEBBLE_CMD_SYNC_SCORE_VAL.toByte())

        Timber.i("Sending sync request to Pebble")

        PebbleKit.sendDataToPebble(this, pebbleAppUUID, dict)
    }
}