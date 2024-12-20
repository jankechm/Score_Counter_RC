package com.mj.scorecounterrc.data.manager

import com.mj.scorecounterrc.Constants
import com.mj.scorecounterrc.data.model.Score
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object ScoreManager {
    private val _localScore = MutableStateFlow(Score(0,0))
    val localScore: StateFlow<Score> = _localScore.asStateFlow()

    /**
     * Previous local score value.
     */
    private var prevLocalScore = _localScore.value.copy()

    /**
     * Timestamp of most recent version of local score. It should be used when syncing score between
     * all devices.
     */
    var timestamp: Long = 0L

    /**
     * Local score is only updated when the activity that uses it exists, therefore store
     * score that was received from smartwatch or Score Counter separately.
     *
     * Constants.MAX_SCORE + 1 as initial value, because I want the initial value to be skipped
     * by the collector.
     *
     */
    private val _receivedScore = MutableStateFlow(
        Score(Constants.MAX_SCORE + 1,Constants.MAX_SCORE + 1)
    )
    val receivedScore: StateFlow<Score> = _receivedScore.asStateFlow()

    fun incrementLeftScore() {
        _localScore.update {
            if (it.left < Constants.MAX_SCORE) {
                it.copy(left = it.left + 1)
            }
            else {
                it.copy(left = Constants.MIN_SCORE)
            }
        }
    }

    fun incrementRightScore() {
        _localScore.update {
            if (it.right < Constants.MAX_SCORE) {
                it.copy(right = it.right + 1)
            }
            else {
                it.copy(right = Constants.MIN_SCORE)
            }
        }
    }

    fun decrementLeftScore() {
        _localScore.update {
            if (it.left > Constants.MIN_SCORE) {
                it.copy(left = it.left - 1)
            }
            else {
                it.copy(left = Constants.MAX_SCORE)
            }
        }
    }

    fun decrementRightScore() {
        _localScore.update {
            if (it.right > Constants.MIN_SCORE) {
                it.copy(right = it.right - 1)
            }
            else {
                it.copy(right = Constants.MAX_SCORE)
            }
        }
    }

    fun setScore(left: Int, right: Int) {
        _localScore.update {
            Score(left, right)
        }
    }

    fun resetScore() {
        _localScore.update {
            Score(0,0)
        }
    }

    fun swapScore() {
        _localScore.update {
            Score(it.right, it.left)
        }
    }

    @Synchronized
    fun confirmNewScore(setNewTimestamp: Boolean) {
        prevLocalScore = _localScore.value.copy()

        if (setNewTimestamp) {
            timestamp = System.currentTimeMillis() / 1000
        }
    }

    fun revertScore() {
        _localScore.update {
            prevLocalScore.copy()
        }
    }

    fun saveReceivedScore(score: Score, timestamp: Long) {
        _receivedScore.update {
            score.copy()
        }
        ScoreManager.timestamp = timestamp
    }
}