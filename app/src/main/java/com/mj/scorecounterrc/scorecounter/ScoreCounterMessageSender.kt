package com.mj.scorecounterrc.scorecounter

import com.mj.scorecounterrc.data.model.Score

interface ScoreCounterMessageSender {
    fun sendScore(score: Score, timestamp: Long)
    fun requestScoreSync()
}