package com.mj.scorecounterrc.data.manager

import com.mj.scorecounterrc.communication.scorecounter.ScoreCounterConnectionManager
import com.mj.scorecounterrc.communication.scorecounter.listener.SCCMListener
import com.mj.scorecounterrc.data.model.ScoreCounterCfg
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScoreCounterCfgManager @Inject constructor(
    private val sccm: ScoreCounterConnectionManager
) {
    private val _scCfg = MutableStateFlow(ScoreCounterCfg())
    val scCfg: StateFlow<ScoreCounterCfg> = _scCfg.asStateFlow()

    private val _isPersisted = MutableStateFlow(true)
    val isPersisted: StateFlow<Boolean> = _isPersisted.asStateFlow()


    private val sccmListener by lazy {
        SCCMListener().apply {
            onCfgReceived = { cfg ->
                _scCfg.value = cfg
            }
            onSentCfgAck = { _isPersisted.value = true }
        }
    }


    init {
        sccm.registerListener(sccmListener)
    }


    fun setBrightnessLevel(level: Int) {
        _scCfg.update { it.copy(brightness = level) }
        sccm.sendBrightnessSetting(level)
        _isPersisted.value = false
    }

    fun setUseScore(useScore: Boolean) {
        _scCfg.update { it.copy(useScore = useScore) }
        sccm.sendShowScoreSetting(useScore)
        _isPersisted.value = false
    }

    fun setUseTime(useTime: Boolean) {
        _scCfg.update { it.copy(useTime = useTime) }
        sccm.sendShowTimeSetting(useTime)
        _isPersisted.value = false
    }

    fun setScroll(scroll: Boolean) {
        _scCfg.update { it.copy(scroll = scroll) }
        sccm.sendScrollSetting(scroll)
        _isPersisted.value = false
    }

    fun loadPersistedScCfg() {
        sccm.sendGetConfigRequest()
    }

    fun persistScCfg() {
        sccm.sendPersistConfig(_scCfg.value)
    }
}