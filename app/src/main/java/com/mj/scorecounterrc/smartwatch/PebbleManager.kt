package com.mj.scorecounterrc.smartwatch

import android.content.Context
import com.getpebble.android.kit.PebbleKit
import com.getpebble.android.kit.util.PebbleDictionary
import com.mj.scorecounterrc.Constants
import com.mj.scorecounterrc.data.model.Score
import com.mj.scorecounterrc.smartwatch.listener.PebbleListener
import timber.log.Timber
import java.util.UUID

object PebbleManager {

    val pebbleAppUUID: UUID = UUID.fromString(Constants.PEBBLE_APP_UUID)

    val pebbleListener by lazy {
        PebbleListener().apply {
            onDataReceived = { pebbleDict ->
                handleReceivedPebbleData(pebbleDict)
            }
        }
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

    private fun handleReceivedPebbleData(pebbleDict: PebbleDictionary?) {
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
                            var msgType = SmartwatchManager.MsgTypeFromSmartwatch.SET_SCORE
                            if (pebbleCmdKeyInt == Constants.FROM_PEBBLE_CMD_SYNC_SCORE_VAL) {
                                msgType = SmartwatchManager.MsgTypeFromSmartwatch.SYNC
                            }
                            SmartwatchManager.handleReceivedData(
                                Score(score1, score2), timestamp, msgType)
                        }
                    }
                }
            }
        }
    }

    fun startPebbleApp(context: Context) {
        PebbleKit.startAppOnPebble(context, pebbleAppUUID)
    }
}