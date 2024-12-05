package com.mj.scorecounterrc.viewmodel

import androidx.lifecycle.ViewModel
import javax.inject.Inject

class EnableRequestSharedViewModel @Inject constructor() : ViewModel() {
    var requestEnableBluetoothCallback: (() -> Unit)? = null

    private fun requestEnableBluetooth() {
        requestEnableBluetoothCallback?.invoke()
    }

    fun onEvent(event: EnableRequestedEvent) {
        when (event) {
            is EnableRequestedEvent.EnableBluetooth -> requestEnableBluetooth()
        }
    }

    sealed interface EnableRequestedEvent {
        data object EnableBluetooth : EnableRequestedEvent
    }
}