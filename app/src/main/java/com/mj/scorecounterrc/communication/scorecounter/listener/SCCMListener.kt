package com.mj.scorecounterrc.communication.scorecounter.listener

import com.mj.scorecounterrc.data.model.ScoreCounterCfg

class SCCMListener {
    var onCfgReceived: ((ScoreCounterCfg) -> Unit)? = null
    var onSentCfgAck: (() -> Unit)? = null
}