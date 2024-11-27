package com.mj.scorecounterrc.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ScoreCounterViewModel : ViewModel() {
    private val _isScFacingToTheReferee = MutableStateFlow(false)
    val isScFacingToTheReferee: StateFlow<Boolean> = _isScFacingToTheReferee.asStateFlow()

    /**
     * Previous BLE display orientation value
     */
    private var wasFacingToTheReferee = _isScFacingToTheReferee.value


//    fun toggleOrientation() {
//        _isFacingToTheReferee.value = !_isFacingToTheReferee.value
//    }
//
//    fun confirmOrientation() {
//        wasFacingToTheReferee = _isFacingToTheReferee.value
//    }
//
//    fun revertOrientation() {
//        _isFacingToTheReferee.value = wasFacingToTheReferee
//    }
//
//    fun setOrientation(isFacingToTheReferee: Boolean) {
//        _isFacingToTheReferee.value = isFacingToTheReferee
//    }

    fun onEvent(event: ScoreCounterEvent) {
        when (event) {
            ScoreCounterEvent.ToggleOrientation -> {
                _isScFacingToTheReferee.value = !_isScFacingToTheReferee.value
            }
            ScoreCounterEvent.ConfirmOrientation -> {
                wasFacingToTheReferee = _isScFacingToTheReferee.value
            }
            ScoreCounterEvent.RevertOrientation -> {
                _isScFacingToTheReferee.value = wasFacingToTheReferee
            }
            is ScoreCounterEvent.SetOrientation -> {
                _isScFacingToTheReferee.value = event.isFacingToTheReferee
            }
        }
    }
}

sealed interface ScoreCounterEvent {
    data object ToggleOrientation : ScoreCounterEvent
    data object ConfirmOrientation : ScoreCounterEvent
    data object RevertOrientation : ScoreCounterEvent
    data class SetOrientation(val isFacingToTheReferee: Boolean) : ScoreCounterEvent
}