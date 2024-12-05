package com.mj.scorecounterrc.smartwatch

import android.app.Application.RECEIVER_EXPORTED
import android.content.Context
import android.content.IntentFilter
import android.os.Build
import com.getpebble.android.kit.PebbleKit
import com.mj.scorecounterrc.ScoreSync
import com.mj.scorecounterrc.broadcastreceiver.SCPebbleDataReceiver
import com.mj.scorecounterrc.scorecounter.ScoreCounterConnectionManager
import com.mj.scorecounterrc.data.manager.ScoreManager
import com.mj.scorecounterrc.data.model.Score
import dagger.hilt.android.qualifiers.ApplicationContext

object SmartwatchManager {

    @ApplicationContext
    private lateinit var context: Context

    private val scPebbleDataReceiver = SCPebbleDataReceiver(PebbleManager.pebbleAppUUID)

    enum class MsgTypeFromSmartwatch {
        SET_SCORE,
        SYNC
    }


    init {
        registerListeners()
        registerReceivers()
        startSmartwatchApp()
    }

    private fun registerReceivers() {
        context.let { app ->
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

    private fun registerListeners() {
        scPebbleDataReceiver.registerListener(PebbleManager.pebbleListener)
    }

    fun sendScoreToSmartwatch(score: Score, timestamp: Long) {
        context.let { app ->
            PebbleManager.sendScoreToPebble(score, timestamp, app)
        }
    }

    fun sendSyncRequestToSmartwatch() {
        context.let { app ->
            PebbleManager.sendSyncRequestToPebble(app)
        }
    }

    fun startSmartwatchApp() {
        context.let { app ->
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