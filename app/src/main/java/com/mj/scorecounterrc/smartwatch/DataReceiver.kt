package com.mj.scorecounterrc.smartwatch

import com.mj.scorecounterrc.data.model.Score

interface DataReceiver {
    fun onDataReceived(score: Score, timestamp: Long, msgType: MsgTypeFromSmartwatch)
}