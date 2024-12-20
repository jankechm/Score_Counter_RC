package com.mj.scorecounterrc.smartwatch.listener

import com.mj.scorecounterrc.data.model.Score
import com.mj.scorecounterrc.smartwatch.MsgTypeFromSmartwatch

class SmartwatchListener {
    var onReceivedDataValidated: ((
        score: Score,
        timestamp: Long,
        msgType: MsgTypeFromSmartwatch)
    -> Unit)? = null
}