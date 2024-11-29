package com.mj.scorecounterrc.smartwatch

import android.app.Application.RECEIVER_EXPORTED
import android.content.IntentFilter
import android.os.Build
import com.getpebble.android.kit.PebbleKit
import com.mj.scorecounterrc.ScoreCounterRcApp
import com.mj.scorecounterrc.ScoreSync
import com.mj.scorecounterrc.broadcastreceiver.SCPebbleDataReceiver
import com.mj.scorecounterrc.scorecounter.ScoreCounterConnectionManager
import com.mj.scorecounterrc.data.manager.ScoreManager
import com.mj.scorecounterrc.data.model.Score

object SmartwatchManager {

    // Should be injected at app.onCreate()
    var app: ScoreCounterRcApp? = null

    private val scPebbleDataReceiver = SCPebbleDataReceiver(PebbleManager.pebbleAppUUID)

    enum class MsgTypeFromSmartwatch {
        SET_SCORE,
        SYNC
    }

    fun registerReceivers() {
        app?.let { app ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                app.registerReceiver(
                    scPebbleDataReceiver,
                    IntentFilter(com.getpebble.android.kit.Constants.INTENT_APP_RECEIVE),
                    RECEIVER_EXPORTED
                )
            } else {
                PebbleKit.registerReceivedDataHandler(app, scPebbleDataReceiver)
            }
        }
    }

    fun registerListeners() {
        scPebbleDataReceiver.registerListener(PebbleManager.pebbleListener)
    }

    fun sendScoreToSmartwatch(score: Score, timestamp: Long) {
        app?.let { app ->
            PebbleManager.sendScoreToPebble(score, timestamp, app)
        }
    }

    fun sendSyncRequestToSmartwatch() {
        app?.let { app ->
            PebbleManager.sendSyncRequestToPebble(app)
        }
    }

    fun startSmartwatchApp() {
        app?.let { app ->
            PebbleManager.startPebbleApp(app)
        }
    }

    fun handleReceivedData(score: Score, timestamp: Long, msgType: MsgTypeFromSmartwatch) {
        if (msgType == MsgTypeFromSmartwatch.SET_SCORE) {
            // Accept new score set by smartwatch
            ScoreCounterConnectionManager.sendScoreToScoreCounter(score, timestamp)
            ScoreManager.saveReceivedScore(score, timestamp)
        } else {
            // Continue with sync process
            ScoreSync.setSmartwatchData(score, timestamp)
        }
    }
}