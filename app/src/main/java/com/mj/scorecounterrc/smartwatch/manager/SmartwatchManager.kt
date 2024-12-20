package com.mj.scorecounterrc.smartwatch.manager

import android.app.Application.RECEIVER_EXPORTED
import android.content.Context
import android.content.IntentFilter
import android.os.Build
import com.getpebble.android.kit.PebbleKit
import com.mj.scorecounterrc.broadcastreceiver.SCPebbleDataReceiver
import com.mj.scorecounterrc.data.model.Score
import com.mj.scorecounterrc.smartwatch.DataReceiver
import com.mj.scorecounterrc.smartwatch.listener.PebbleListener
import com.mj.scorecounterrc.smartwatch.listener.SmartwatchListener
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmartwatchManager @Inject constructor(
    @ApplicationContext private var context: Context,
    private val pebbleManager: PebbleManager,
    private val dataReceiver: DataReceiver,
    private val scPebbleDataReceiver: SCPebbleDataReceiver
) {

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
                dataReceiver.onDataReceived(score, timestamp, msgType)
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
}