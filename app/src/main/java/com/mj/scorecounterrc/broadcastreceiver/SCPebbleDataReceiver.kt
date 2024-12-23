package com.mj.scorecounterrc.broadcastreceiver

import android.content.Context
import com.getpebble.android.kit.PebbleKit
import com.getpebble.android.kit.PebbleKit.PebbleDataReceiver
import com.getpebble.android.kit.util.PebbleDictionary
import com.mj.scorecounterrc.smartwatch.listener.PebbleListener
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class SCPebbleDataReceiver(subscribedUuid: UUID?) : PebbleDataReceiver(subscribedUuid) {

    private var listeners: MutableSet<PebbleListener> = ConcurrentHashMap.newKeySet()
    override fun receiveData(context: Context?, transactionId: Int, data: PebbleDictionary?) {
        listeners.forEach { it.onDataReceived?.invoke(data) }

        PebbleKit.sendAckToPebble(context, transactionId)
    }

    fun registerListener(listener: PebbleListener) {
        if (listeners.map { it }.none { it == listener }) {
            listeners.add(listener)

            Timber.d("Added a PebbleListener, " +
                    "${listeners.size} listeners total")
        }
    }

    fun unregisterListener(listener: PebbleListener) {
        listeners.removeIf { it == listener }
        Timber.d("Removed a PebbleListener, ${listeners.size} listeners total")
    }
}