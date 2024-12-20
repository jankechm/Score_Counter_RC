package com.mj.scorecounterrc.smartwatch.manager

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
import com.mj.scorecounterrc.smartwatch.MsgTypeFromSmartwatch
import com.mj.scorecounterrc.smartwatch.listener.PebbleListener
import com.mj.scorecounterrc.smartwatch.listener.SmartwatchListener
import dagger.hilt.android.qualifiers.ApplicationContext

object SmartwatchManager {

    @ApplicationContext
    private lateinit var context: Context

    private val scPebbleDataReceiver = SCPebbleDataReceiver(PebbleManager.pebbleAppUUID)
    // TODO inject
    private val pebbleManager: PebbleManager = PebbleManager()


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
        registerPebbleListeners()
    }

    private fun registerPebbleListeners() {
        pebbleManager.registerListener(SmartwatchListener().apply {
            onReceivedDataValidated = { score, timestamp, msgType ->
                handleReceivedData(score, timestamp, msgType)
            }
        })

        scPebbleDataReceiver.registerListener(PebbleListener().apply {
            onDataReceived = { pebbleDict ->
                pebbleManager.handleReceivedPebbleData(pebbleDict)
            }
        })
    }

    fun sendScoreToSmartwatch(score: Score, timestamp: Long) {
        context.let { app ->
            pebbleManager.sendScoreToPebble(score, timestamp, app)
        }
    }

    fun sendSyncRequestToSmartwatch() {
        context.let { app ->
            pebbleManager.sendSyncRequestToPebble(app)
        }
    }

    private fun startSmartwatchApp() {
        context.let { app ->
            pebbleManager.startPebbleApp(app)
        }
    }

    private fun handleReceivedData(score: Score, timestamp: Long, msgType: MsgTypeFromSmartwatch) {
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