package com.mj.scorecounterrc.communication.scorecounter

import com.mj.scorecounterrc.data.model.Score

interface ScoreCounterMessageSender {
    fun sendScore(score: Score, timestamp: Long)
    fun requestScoreSync()
}