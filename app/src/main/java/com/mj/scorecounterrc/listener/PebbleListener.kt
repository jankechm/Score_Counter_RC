package com.mj.scorecounterrc.listener

import com.getpebble.android.kit.util.PebbleDictionary
import com.mj.scorecounterrc.broadcastreceiver.SCPebbleDataReceiver

/** A listener containing callback methods to be registered with [SCPebbleDataReceiver].*/
class PebbleListener {
    var onDataReceived: ((PebbleDictionary?) -> Unit)? = null
}