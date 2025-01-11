package com.mj.scorecounterrc.viewmodel

import androidx.lifecycle.ViewModel
import com.mj.scorecounterrc.communication.scorecounter.ScoreCounterConnectionManager
import com.mj.scorecounterrc.communication.scorecounter.listener.SCCMListener
import com.mj.scorecounterrc.data.model.ScoreCounterCfg
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val sccm: ScoreCounterConnectionManager
) : ViewModel() {

    private val _loadedSettings: MutableStateFlow<ScoreCounterCfg> =
        MutableStateFlow(ScoreCounterCfg())
    val loadedSettings: StateFlow<ScoreCounterCfg> = _loadedSettings.asStateFlow()

    private val _isPersistBtnEnabled: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isPersistBtnEnabled: StateFlow<Boolean> = _isPersistBtnEnabled.asStateFlow()

    private val sccmListener by lazy {
        SCCMListener().apply {
            onCfgReceived = { cfg ->
                _loadedSettings.value = cfg
            }
            onSentCfgAck = { _isPersistBtnEnabled.value = false }
        }
    }


    init {
        registerListeners()
    }

    private fun registerListeners() {
        sccm.registerListener(sccmListener)
    }

    private fun unregisterListeners() {
        sccm.unregisterListener(sccmListener)
    }

    override fun onCleared() {
        super.onCleared()
        unregisterListeners()
    }


    fun onEvent(event: SettingsViewModelEvent) {
        when (event) {
            SettingsViewModelEvent.RequestLoadDataEvent -> {
                sccm.sendGetConfigRequest()
            }
            is SettingsViewModelEvent.BrightnessChangedEvent -> {
                sccm.sendBrightnessSetting(event.level.toInt())
                _isPersistBtnEnabled.value = true
            }
            is SettingsViewModelEvent.ShowScoreChangedEvent -> {
                sccm.sendShowScoreSetting(event.isOn)
                _isPersistBtnEnabled.value = true
            }
            is SettingsViewModelEvent.ShowTimeChangedEvent -> {
                sccm.sendShowTimeSetting(event.isOn)
                _isPersistBtnEnabled.value = true
            }
            is SettingsViewModelEvent.TextViewBehaviourChangedEvent -> {
                sccm.sendScrollSetting(event.behaviour == TextViewBehaviour.SCROLL)
                _isPersistBtnEnabled.value = true
            }
            is SettingsViewModelEvent.PersistSettingsEvent -> {
                sccm.sendPersistConfig(event.config)
            }
        }
    }

    sealed interface SettingsViewModelEvent {
        data object RequestLoadDataEvent : SettingsViewModelEvent
        data class BrightnessChangedEvent(val level: Float) : SettingsViewModelEvent
        data class ShowScoreChangedEvent(val isOn: Boolean) : SettingsViewModelEvent
        data class ShowTimeChangedEvent(val isOn: Boolean) : SettingsViewModelEvent
        data class TextViewBehaviourChangedEvent(
            val behaviour: TextViewBehaviour) : SettingsViewModelEvent
        data class PersistSettingsEvent(val config: ScoreCounterCfg) : SettingsViewModelEvent
    }

    enum class TextViewBehaviour(val text: String) {
        ALTERNATE("Alternate Text"),
        SCROLL("Scroll Text")
    }
}