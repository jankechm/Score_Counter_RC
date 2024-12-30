package com.mj.scorecounterrc.communication.smartwatch.manager

import android.content.Context
import com.getpebble.android.kit.PebbleKit
import com.getpebble.android.kit.util.PebbleDictionary
import com.mj.scorecounterrc.Constants
import com.mj.scorecounterrc.data.model.Score
import com.mj.scorecounterrc.communication.smartwatch.MsgTypeFromSmartwatch
import com.mj.scorecounterrc.communication.smartwatch.listener.SmartwatchListener
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

class PebbleManager @Inject constructor() {

    companion object {
        val pebbleAppUUID: UUID = UUID.fromString(Constants.PEBBLE_APP_UUID)
    }

    private var listeners: MutableSet<WeakReference<SmartwatchListener>> =
        ConcurrentHashMap.newKeySet()


    fun registerListener(listener: SmartwatchListener) {
        if (listeners.map { it.get() }.none { it?.equals(listener) == true }) {
            listeners.add(WeakReference(listener))
            listeners.removeIf { it.get() == null }

            Timber.d("Added a SmartwatchListener, " +
                    "${listeners.size} listeners total")
        }
    }

    fun unregisterListener(listener: SmartwatchListener) {
        listeners.removeIf { it.get() == listener || it.get() == null }
        Timber.d("Removed a SmartwatchListener, ${listeners.size} listeners total")
    }

    fun sendScoreToPebble(score: Score, timestamp: Long, context: Context) {
//        if (PebbleKit.isWatchConnected(this)) {
        val dict = PebbleDictionary()

        dict.addUint8(Constants.TO_PEBBLE_CMD_KEY, Constants.TO_PEBBLE_CMD_SET_SCORE_VAL.toByte())
        dict.addUint16(Constants.TO_PEBBLE_SCORE_1_KEY, score.left.toShort())
        dict.addUint16(Constants.TO_PEBBLE_SCORE_2_KEY, score.right.toShort())
        // The lower 32 bits should remain the same after timestamp.toInt() conversion.
        dict.addUint32(Constants.TO_PEBBLE_TIMESTAMP_KEY, timestamp.toInt())

        Timber.i("Sending score ${score.left}:${score.right} T=$timestamp to Pebble")

        PebbleKit.sendDataToPebble(context, pebbleAppUUID, dict)
//        }
    }

    fun sendSyncRequestToPebble(context: Context) {
        val dict = PebbleDictionary()

        dict.addUint8(Constants.TO_PEBBLE_CMD_KEY, Constants.TO_PEBBLE_CMD_SYNC_SCORE_VAL.toByte())

        Timber.i("Sending sync request to Pebble")

        PebbleKit.sendDataToPebble(context, pebbleAppUUID, dict)
    }

    fun handleReceivedPebbleData(pebbleDict: PebbleDictionary?) {
        if (pebbleDict == null) {
            Timber.i("Empty PebbleDictionary received.")
        }
        else {
            pebbleDict.forEach { tuple -> Timber.d("Pebble tuple key: ${tuple.key} | " +
                    "value: ${tuple.value}") }
            pebbleDict.getUnsignedIntegerAsLong(Constants.FROM_PEBBLE_CMD_KEY)?.let { pebbleCmdKey ->
                val pebbleCmdKeyInt = pebbleCmdKey.toInt()
                when (pebbleCmdKeyInt) {
                    Constants.FROM_PEBBLE_CMD_SET_SCORE_VAL,
                    Constants.FROM_PEBBLE_CMD_SYNC_SCORE_VAL -> {
                        val score1 = pebbleDict.getUnsignedIntegerAsLong(
                            Constants.FROM_PEBBLE_SCORE_1_KEY)?.toInt()
                        val score2 = pebbleDict.getUnsignedIntegerAsLong(
                            Constants.FROM_PEBBLE_SCORE_2_KEY)?.toInt()
                        val timestamp = pebbleDict.getUnsignedIntegerAsLong(
                            Constants.FROM_PEBBLE_TIMESTAMP_KEY)

                        if (score1 != null && score2 != null && timestamp != null) {
                            var msgType = MsgTypeFromSmartwatch.SET_SCORE
                            if (pebbleCmdKeyInt == Constants.FROM_PEBBLE_CMD_SYNC_SCORE_VAL) {
                                msgType = MsgTypeFromSmartwatch.SYNC
                            }

                            listeners.forEach {
                                it.get()?.onReceivedDataValidated?.invoke(
                                    Score(score1, score2), timestamp, msgType)
                            }
                        }
                    }
                }
            }
        }
    }

    fun startPebbleApp(context: Context) {
        PebbleKit.startAppOnPebble(context, pebbleAppUUID)
    }

    fun stopPebbleApp(context: Context) {
        PebbleKit.closeAppOnPebble(context, pebbleAppUUID)
    }
}