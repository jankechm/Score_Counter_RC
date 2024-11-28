package com.mj.scorecounterrc.broadcastreceiver

import android.content.Context
import com.getpebble.android.kit.PebbleKit
import com.getpebble.android.kit.PebbleKit.PebbleDataReceiver
import com.getpebble.android.kit.util.PebbleDictionary
import com.mj.scorecounterrc.listener.PebbleListener
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class SCPebbleDataReceiver(subscribedUuid: UUID?) : PebbleDataReceiver(subscribedUuid) {

    private var listeners: MutableSet<WeakReference<PebbleListener>> = ConcurrentHashMap.newKeySet()
    override fun receiveData(context: Context?, transactionId: Int, data: PebbleDictionary?) {
        listeners.forEach { it.get()?.onDataReceived?.invoke(data) }

        PebbleKit.sendAckToPebble(context, transactionId)
    }

    fun registerListener(listener: PebbleListener) {
        if (listeners.map { it.get() }.none { it?.equals(listener) == true }) {
            listeners.add(WeakReference(listener))
            listeners.removeIf { it.get() == null }

            Timber.d("Added a PebbleListener, " +
                    "${listeners.size} listeners total")
        }
    }

    fun unregisterListener(listener: PebbleListener) {
        listeners.removeIf { it.get() == listener || it.get() == null }
        Timber.d("Removed a PebbleListener, ${listeners.size} listeners total")
    }
}