package com.mj.scorecounterrc.viewmodel

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.ViewModel
import com.mj.scorecounterrc.ble.ConnectionManager
import com.mj.scorecounterrc.communication.smartwatch.manager.SmartwatchManager
import com.mj.scorecounterrc.service.RcService
import dagger.hilt.android.lifecycle.HiltViewModel
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class CloseAppViewModel @Inject constructor(
    private val smartwatchManager: SmartwatchManager
) : ViewModel() {

    fun onEvent(event: CloseAppViewModelEvent) {
        when (event) {
            is CloseAppViewModelEvent.ConfirmButtonClickedEvent -> {
                Timber.d("Closing the app with back button.")
                event.activity.stopService(Intent(event.activity, RcService::class.java))
                ConnectionManager.disconnectAllDevices()
                smartwatchManager.stopPebbleApp()
                event.activity.finishAndRemoveTask()
            }
        }
    }

    sealed interface CloseAppViewModelEvent {
        data class ConfirmButtonClickedEvent(val activity: Activity) : CloseAppViewModelEvent
    }
}