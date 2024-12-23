package com.mj.scorecounterrc.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mj.scorecounterrc.Constants
import com.mj.scorecounterrc.ble.ConnectionManager
import com.mj.scorecounterrc.data.manager.ScoreManager
import com.mj.scorecounterrc.data.manager.StorageManager
import com.mj.scorecounterrc.data.model.Score
import com.mj.scorecounterrc.listener.ConnectionEventListener
import com.mj.scorecounterrc.scorecounter.ScoreCounterConnectionManager
import com.mj.scorecounterrc.smartwatch.manager.SmartwatchManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ScoreCounterViewModel @Inject constructor(
    private val smartwatchManager: SmartwatchManager,
    private val scoreCounterConnectionManager: ScoreCounterConnectionManager,
    private val storageManager: StorageManager
): ViewModel() {

    private val _isScFacingToTheReferee = MutableStateFlow(false)
    val isScFacingToTheReferee: StateFlow<Boolean> = _isScFacingToTheReferee.asStateFlow()

    /**
     * Previous BLE display orientation value
     */
    private var wasFacingToTheReferee = _isScFacingToTheReferee.value

    private val _scoreCounterState = MutableStateFlow(ScoreCounterState.IDLE)
    val scoreCounterState: StateFlow<ScoreCounterState> = _scoreCounterState.asStateFlow()


    private val connectionEventListener = ConnectionEventListener().apply {
        onDisconnect = { _ ->
            if (_scoreCounterState.value == ScoreCounterState.SCORE_SENT) {
                // The buttons no longer need to be visible - set IDLE state
                _scoreCounterState.update { ScoreCounterState.IDLE }
            }
        }
        onCharacteristicWrite = { _, _ ->
            _scoreCounterState.update { ScoreCounterState.IDLE }
        }
    }


    init {
        ConnectionManager.registerListener(connectionEventListener)
        loadPersistedScore()

        // Update local score based on received score.
        viewModelScope.launch {
            ScoreManager.receivedScore.collect {
                // Skip the initial value to avoid conflict with loaded score from persisted
                // storage
                if (it.left <= Constants.MAX_SCORE && it.right <= Constants.MAX_SCORE) {
                    // Determining which score is left and which is right to the phone's
                    // perspective.
                    if (isScFacingToTheReferee.value) {
                        ScoreManager.setScore(it.left, it.right)
                    } else {
                        ScoreManager.setScore(it.right, it.left)
                    }
                    ScoreManager.confirmNewScore(false)

                    val localScore = ScoreManager.localScore.value
                    persistScore(localScore.left, localScore.right, isScFacingToTheReferee.value,
                        ScoreManager.timestamp)
                }
            }
        }
    }

    fun onEvent(event: ScoreCounterEvent) {
        when (event) {
            ScoreCounterEvent.ToggleOrientation -> {
                _isScFacingToTheReferee.update { !it }
                _scoreCounterState.update { ScoreCounterState.SCORE_CHANGED }
            }
            ScoreCounterEvent.SwapScore -> {
                ScoreManager.swapScore()
                _scoreCounterState.update { ScoreCounterState.SCORE_CHANGED }
            }
            ScoreCounterEvent.IncrementLeftScore -> {
                ScoreManager.incrementLeftScore()
                _scoreCounterState.update { ScoreCounterState.SCORE_CHANGED }
            }
            ScoreCounterEvent.IncrementRightScore -> {
                ScoreManager.incrementRightScore()
                _scoreCounterState.update { ScoreCounterState.SCORE_CHANGED }
            }
            ScoreCounterEvent.DecrementLeftScore -> {
                ScoreManager.decrementLeftScore()
                _scoreCounterState.update { ScoreCounterState.SCORE_CHANGED }
            }
            ScoreCounterEvent.DecrementRightScore -> {
                ScoreManager.decrementRightScore()
                _scoreCounterState.update { ScoreCounterState.SCORE_CHANGED }
            }
            ScoreCounterEvent.ResetButtonClicked -> {
                ScoreManager.resetScore()
                _scoreCounterState.update { ScoreCounterState.SCORE_CHANGED }
            }
            ScoreCounterEvent.CancelButtonClicked -> {
                ScoreManager.revertScore()
                _isScFacingToTheReferee.update { wasFacingToTheReferee }
                _scoreCounterState.update { ScoreCounterState.IDLE }

            }
            ScoreCounterEvent.OkButtonClicked -> {
                val score = ScoreManager.localScore.value
                var score1 = score.left
                var score2 = score.right

                if (!_isScFacingToTheReferee.value) {
                    score1 = score.right
                    score2 = score.left
                }

                Timber.d("Confirm score button clicked. Score is $score1:$score2")

                ScoreManager.confirmNewScore(true)

                Timber.d("Local score timestamp: ${ScoreManager.timestamp}")

                val isSentToSc = scoreCounterConnectionManager.sendScoreToScoreCounter(
                    Score(score1, score2), ScoreManager.timestamp)
                smartwatchManager.sendScoreToSmartwatch(
                    Score(score1, score2), ScoreManager.timestamp)

                if (isSentToSc) {
                    _scoreCounterState.update { ScoreCounterState.SCORE_SENT }
                } else {
                    _scoreCounterState.update { ScoreCounterState.IDLE }
                }

                // Confirm also SC orientation
                wasFacingToTheReferee = _isScFacingToTheReferee.value

                persistScore(
                    score.left, score.right, _isScFacingToTheReferee.value, ScoreManager.timestamp
                )
            }
        }
    }

    private fun persistScore(score1: Int, score2: Int, isFacingToTheReferee: Boolean,
                             timestamp: Long) {
        storageManager.saveScore1(score1)
        storageManager.saveScore2(score2)
        storageManager.saveSCOrientation(isFacingToTheReferee)
        storageManager.saveTimestamp(timestamp)
    }

    fun loadPersistedScore() {
        val score1 = storageManager.getScore1()
        val score2 = storageManager.getScore2()
        val isFacingToTheReferee = storageManager.getSCOrientation()
        val timestamp = storageManager.getTimestamp()

        ScoreManager.setScore(score1, score2)
        ScoreManager.timestamp = timestamp
        ScoreManager.confirmNewScore(false)
        _isScFacingToTheReferee.update { isFacingToTheReferee }
    }
}

sealed interface ScoreCounterEvent {
    data object ToggleOrientation : ScoreCounterEvent
    data object SwapScore : ScoreCounterEvent
    data object IncrementLeftScore : ScoreCounterEvent
    data object IncrementRightScore : ScoreCounterEvent
    data object DecrementLeftScore : ScoreCounterEvent
    data object DecrementRightScore : ScoreCounterEvent
    data object ResetButtonClicked : ScoreCounterEvent
    data object OkButtonClicked : ScoreCounterEvent
    data object CancelButtonClicked : ScoreCounterEvent
}

enum class ScoreCounterState {
    IDLE, // = OK and Cancel buttons not visible and not clickable
    SCORE_CHANGED,
    SCORE_SENT,
}