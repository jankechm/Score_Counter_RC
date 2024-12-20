package com.mj.scorecounterrc

import com.mj.scorecounterrc.data.model.Score

interface ScoreSync {
    fun trySync()
    fun setScoreCounterData(score: Score, timestamp: Long)
}