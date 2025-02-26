package com.mj.scorecounterrc

import android.os.Handler
import android.os.Looper
import com.mj.scorecounterrc.data.manager.ScoreManager
import com.mj.scorecounterrc.data.model.Score
import com.mj.scorecounterrc.communication.scorecounter.ScoreCounterMessageSender
import com.mj.scorecounterrc.communication.smartwatch.MsgTypeFromSmartwatch
import com.mj.scorecounterrc.communication.smartwatch.manager.SmartwatchManager
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class ScoreSyncImpl @Inject constructor(
    private val smartwatchManager: Provider<SmartwatchManager>,
    private val scoreCounterMessageSender: Provider<ScoreCounterMessageSender>
) : ScoreSync {

    private val waitingForWatchData: AtomicBoolean = AtomicBoolean(false)
    private val waitingForSCData: AtomicBoolean = AtomicBoolean(false)

    private val watchDataReceived = AtomicBoolean(false)
    private val scDataReceived = AtomicBoolean(false)

    private var getWatchDataAttempt = AtomicInteger(0)
    private var getSCDataAttempt = AtomicInteger(0)

    private var watchScore = Score(0,0)
    private var scoreCounterScore = Score(0,0)

    private var watchTimestamp: Long = 0
    private var scoreCounterTimestamp: Long = 0

    companion object {
        const val GET_WATCH_DATA_MAX_ATTEMPTS = 2
        const val GET_SC_DATA_MAX_ATTEMPTS = 2
        const val GET_WATCH_DATA_TIMEOUT = 1000L
        const val GET_SC_DATA_TIMEOUT = 1000L
    }

    private val handler = Handler(Looper.getMainLooper())

    /**
     * Send sync request to smartwatch and wait for it until [GET_WATCH_DATA_TIMEOUT] expires
     * up to [GET_WATCH_DATA_MAX_ATTEMPTS] times.
     */
    private val getWatchDataTimerRunnable = object : Runnable {
        override fun run() {
            if (getWatchDataAttempt.getAndIncrement() < GET_WATCH_DATA_MAX_ATTEMPTS) {
                smartwatchManager.get().sendSyncRequestToSmartwatch()
                handler.postDelayed(this, GET_WATCH_DATA_TIMEOUT)
            }
        }
    }

    /**
     * Send sync request to Score Counter and wait for it until [GET_SC_DATA_TIMEOUT] expires
     * up to [GET_SC_DATA_MAX_ATTEMPTS] times.
     */
    private val getSCDataTimerRunnable = object : Runnable {
        override fun run() {
            if (getSCDataAttempt.getAndIncrement() < GET_SC_DATA_MAX_ATTEMPTS) {
                scoreCounterMessageSender.get().requestScoreSync()
                handler.postDelayed(this, GET_SC_DATA_TIMEOUT)
            }
        }
    }


    @Synchronized
    override fun trySync() {
        if (isReady()) {
            fullSync()
            reset()
            Timber.i("Full sync done")
        } else {
            if (!watchDataReceived.get() && !waitingForWatchData.get()) {
                handler.postDelayed(getWatchDataTimerRunnable, GET_WATCH_DATA_TIMEOUT)
                waitingForWatchData.set(true)
            }
            if (!scDataReceived.get() && !waitingForSCData.get()) {
                handler.postDelayed(getSCDataTimerRunnable, GET_SC_DATA_TIMEOUT)
                waitingForSCData.set(true)
            }

            if (watchDataReceived.get() && getSCDataAttempt.get() > GET_SC_DATA_MAX_ATTEMPTS) {
                syncWatchAndPhone()
                reset()
                Timber.i("Partial sync done: watch and phone.")
            } else if (scDataReceived.get() && getWatchDataAttempt.get() > GET_WATCH_DATA_MAX_ATTEMPTS) {
                syncSCAndPhone()
                reset()
                Timber.i("Partial sync done: score counter and phone.")
            } else if (getSCDataAttempt.get() > GET_SC_DATA_MAX_ATTEMPTS
                && getWatchDataAttempt.get() > GET_WATCH_DATA_MAX_ATTEMPTS
            ) {
                reset()
            }
        }
    }

    private fun fullSync() {
        if (ScoreManager.timestamp == watchTimestamp &&
            ScoreManager.timestamp == scoreCounterTimestamp) {
            // Everything already in sync.
            return
        }

        if (watchTimestamp >= ScoreManager.timestamp && watchTimestamp >= scoreCounterTimestamp) {
            // Smartwatch has the latest score, propagate it.
            ScoreManager.saveReceivedScore(watchScore, watchTimestamp)
            scoreCounterMessageSender.get().sendScore(watchScore, watchTimestamp)
        } else if (ScoreManager.timestamp >= watchTimestamp
            && ScoreManager.timestamp >= scoreCounterTimestamp) {
            // Smartphone has the latest score, propagate it to those, who's timestamp is not equal.
            smartwatchManager.get().sendScoreToSmartwatch(ScoreManager.localScore.value, ScoreManager.timestamp)
            if (ScoreManager.timestamp > scoreCounterTimestamp) {
                scoreCounterMessageSender.get().sendScore(
                    ScoreManager.localScore.value,
                    ScoreManager.timestamp
                )
            }
        } else {
            // Score Counter has the latest score, propagate it.
            ScoreManager.saveReceivedScore(scoreCounterScore, scoreCounterTimestamp)
            smartwatchManager.get().sendScoreToSmartwatch(scoreCounterScore, scoreCounterTimestamp)
        }
    }

    private fun syncWatchAndPhone() {
        if (ScoreManager.timestamp > watchTimestamp) {
            smartwatchManager.get().sendScoreToSmartwatch(ScoreManager.localScore.value, ScoreManager.timestamp)
        } else if (ScoreManager.timestamp < watchTimestamp) {
            ScoreManager.saveReceivedScore(watchScore, watchTimestamp)
        }
    }

    private fun syncSCAndPhone() {
        if (ScoreManager.timestamp > scoreCounterTimestamp) {
            scoreCounterMessageSender.get().sendScore(ScoreManager.localScore.value, ScoreManager.timestamp)
        } else if (ScoreManager.timestamp < scoreCounterTimestamp) {
            ScoreManager.saveReceivedScore(scoreCounterScore, scoreCounterTimestamp)
        }
    }

    private fun isReady(): Boolean {
        return watchDataReceived.get() && scDataReceived.get()
    }

    override fun setScoreCounterData(score: Score, timestamp: Long) {
        scoreCounterScore = score
        scoreCounterTimestamp = timestamp

        scDataReceived.set(true)
        waitingForSCData.set(false)

        handler.removeCallbacks(getSCDataTimerRunnable)

        getSCDataAttempt.set(0)

        trySync()
    }

    private fun setSmartwatchData(score: Score, timestamp: Long) {
        watchScore = score
        watchTimestamp = timestamp

        watchDataReceived.set(true)
        waitingForWatchData.set(false)

        handler.removeCallbacks(getWatchDataTimerRunnable)

        getWatchDataAttempt.set(0)

        trySync()
    }

    private fun reset() {
        waitingForWatchData.set(false)
        waitingForSCData.set(false)

        watchDataReceived.set(false)
        scDataReceived.set(false)

        handler.removeCallbacks(getSCDataTimerRunnable)
        handler.removeCallbacks(getWatchDataTimerRunnable)

        getWatchDataAttempt.set(0)
        getSCDataAttempt.set(0)
    }

    fun onDataReceived(score: Score, timestamp: Long, msgType: MsgTypeFromSmartwatch) {
        if (msgType == MsgTypeFromSmartwatch.SET_SCORE) {
            // Accept new score set by smartwatch
            scoreCounterMessageSender.get().sendScore(score, timestamp)
            ScoreManager.saveReceivedScore(score, timestamp)
        } else {
            // Continue with sync process
            setSmartwatchData(score, timestamp)
        }
    }
}