package com.mj.blescorecounterremotecontroller.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DisplayViewModel : ViewModel() {
    private val _isFacingToTheReferee = MutableStateFlow(false)
    val isFacingToTheReferee: StateFlow<Boolean> = _isFacingToTheReferee.asStateFlow()

    /**
     * Previous BLE display orientation value
     */
    private var wasFacingToTheReferee = _isFacingToTheReferee.value


    fun toggleOrientation() {
        _isFacingToTheReferee.value = !_isFacingToTheReferee.value
//        ScoreManager.swapScore()
    }

    fun confirmOrientation() {
        wasFacingToTheReferee = _isFacingToTheReferee.value
    }

    fun revertOrientation() {
        _isFacingToTheReferee.value = wasFacingToTheReferee
    }

    fun setOrientation(isFacingToTheReferee: Boolean) {
        _isFacingToTheReferee.value = isFacingToTheReferee
    }
}