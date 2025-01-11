package com.mj.scorecounterrc.viewmodel

import androidx.lifecycle.ViewModel
import com.mj.scorecounterrc.communication.scorecounter.ScoreCounterConnectionManager
import com.mj.scorecounterrc.communication.scorecounter.listener.SCCMListener
import com.mj.scorecounterrc.data.manager.AppCfgManager
import com.mj.scorecounterrc.data.model.AppCfg
import com.mj.scorecounterrc.data.model.ScoreCounterCfg
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val sccm: ScoreCounterConnectionManager,
    private val appCfgManager: AppCfgManager
) : ViewModel() {

    private val _loadedScSettings: MutableStateFlow<ScoreCounterCfg> =
        MutableStateFlow(ScoreCounterCfg())
    val loadedScSettings: StateFlow<ScoreCounterCfg> = _loadedScSettings.asStateFlow()

    private val _isScPersistBtnEnabled: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isScPersistBtnEnabled: StateFlow<Boolean> = _isScPersistBtnEnabled.asStateFlow()

    val appCfg: StateFlow<AppCfg> = appCfgManager.appCfg

    private val sccmListener by lazy {
        SCCMListener().apply {
            onCfgReceived = { cfg ->
                _loadedScSettings.value = cfg
            }
            onSentCfgAck = { _isScPersistBtnEnabled.value = false }
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
            SettingsViewModelEvent.RequestLoadScConfigEvent -> {
                sccm.sendGetConfigRequest()
            }
            is SettingsViewModelEvent.BrightnessChangedEvent -> {
                sccm.sendBrightnessSetting(event.level.toInt())
                _isScPersistBtnEnabled.value = true
            }
            is SettingsViewModelEvent.ShowScoreChangedEvent -> {
                sccm.sendShowScoreSetting(event.isOn)
                _isScPersistBtnEnabled.value = true
            }
            is SettingsViewModelEvent.ShowTimeChangedEvent -> {
                sccm.sendShowTimeSetting(event.isOn)
                _isScPersistBtnEnabled.value = true
            }
            is SettingsViewModelEvent.AutoConnectOnStartupChangedEvent -> {
                appCfgManager.setAutoConnectOnStart(event.isOn)
            }
            is SettingsViewModelEvent.TextViewBehaviourChangedEvent -> {
                sccm.sendScrollSetting(event.behaviour == TextViewBehaviour.SCROLL)
                _isScPersistBtnEnabled.value = true
            }
            is SettingsViewModelEvent.PersistScSettingsEvent -> {
                sccm.sendPersistConfig(event.config)
            }
            is SettingsViewModelEvent.PersistAppCfgEvent -> {
                appCfgManager.persistAppCfg()
            }
        }
    }

    sealed interface SettingsViewModelEvent {
        data object RequestLoadScConfigEvent : SettingsViewModelEvent
        data class BrightnessChangedEvent(val level: Float) : SettingsViewModelEvent
        data class ShowScoreChangedEvent(override var isOn: Boolean) : SettingsViewModelSwitchEvent(isOn)
        data class ShowTimeChangedEvent(override var isOn: Boolean) : SettingsViewModelSwitchEvent(isOn)
        data class AutoConnectOnStartupChangedEvent(override var isOn: Boolean) : SettingsViewModelSwitchEvent(isOn)
        data class TextViewBehaviourChangedEvent(
            val behaviour: TextViewBehaviour) : SettingsViewModelEvent
        data class PersistScSettingsEvent(val config: ScoreCounterCfg) : SettingsViewModelEvent
        data object PersistAppCfgEvent : SettingsViewModelEvent
    }

    sealed class SettingsViewModelSwitchEvent(open var isOn: Boolean) : SettingsViewModelEvent

    enum class TextViewBehaviour(val text: String) {
        ALTERNATE("Alternate Text"),
        SCROLL("Scroll Text")
    }
}