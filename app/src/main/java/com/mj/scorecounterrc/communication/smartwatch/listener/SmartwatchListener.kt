package com.mj.scorecounterrc.communication.smartwatch.listener

import com.mj.scorecounterrc.data.model.Score
import com.mj.scorecounterrc.communication.smartwatch.MsgTypeFromSmartwatch

class SmartwatchListener {
    var onReceivedDataValidated: ((
        score: Score,
        timestamp: Long,
        msgType: MsgTypeFromSmartwatch
    )
    -> Unit)? = null
}