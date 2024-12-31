package com.mj.scorecounterrc.communication.smartwatch.manager

import android.app.Application.RECEIVER_EXPORTED
import android.content.Context
import android.content.IntentFilter
import android.os.Build
import com.getpebble.android.kit.PebbleKit
import com.mj.scorecounterrc.ScoreSyncImpl
import com.mj.scorecounterrc.broadcastreceiver.SCPebbleDataReceiver
import com.mj.scorecounterrc.data.model.Score
import com.mj.scorecounterrc.communication.smartwatch.listener.PebbleListener
import com.mj.scorecounterrc.communication.smartwatch.listener.SmartwatchListener
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmartwatchManager @Inject constructor(
    @ApplicationContext private var context: Context,
    private val pebbleManager: PebbleManager,
    private val scoreSync: ScoreSyncImpl,
    private val scPebbleDataReceiver: SCPebbleDataReceiver
) {

    private val scoreSyncSmartWatchListener = SmartwatchListener().apply {
        onReceivedDataValidated = { score, timestamp, msgType ->
            scoreSync.onDataReceived(score, timestamp, msgType)
        }
    }

    init {
        registerListeners()
        registerReceivers()
    }

    private fun registerReceivers() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(
                scPebbleDataReceiver,
                IntentFilter(com.getpebble.android.kit.Constants.INTENT_APP_RECEIVE),
                RECEIVER_EXPORTED
            )
        } else {
            PebbleKit.registerReceivedDataHandler(context, scPebbleDataReceiver)
        }
    }

    private fun registerListeners() {
        registerPebbleListeners()
    }

    private fun registerPebbleListeners() {
        pebbleManager.registerListener(scoreSyncSmartWatchListener)

        scPebbleDataReceiver.registerListener(PebbleListener().apply {
            onDataReceived = { pebbleDict ->
                pebbleManager.handleReceivedPebbleData(pebbleDict)
            }
        })
    }

    fun sendScoreToSmartwatch(score: Score, timestamp: Long) {
        pebbleManager.sendScoreToPebble(score, timestamp, context)
    }

    fun sendSyncRequestToSmartwatch() {
        pebbleManager.sendSyncRequestToPebble(context)
    }

    fun startSmartwatchApp() {
        pebbleManager.startPebbleApp(context)
    }

    fun stopSmartwatchApp() {
        pebbleManager.stopPebbleApp(context)
    }
}