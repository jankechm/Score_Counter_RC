package com.mj.scorecounterrc.viewmodel

import androidx.lifecycle.ViewModel
import com.mj.scorecounterrc.data.manager.AppCfgManager
import com.mj.scorecounterrc.data.manager.ScoreCounterCfgManager
import com.mj.scorecounterrc.data.model.AppCfg
import com.mj.scorecounterrc.data.model.ScoreCounterCfg
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val scCfgManager: ScoreCounterCfgManager,
    private val appCfgManager: AppCfgManager
) : ViewModel() {

    val scCfg: StateFlow<ScoreCounterCfg> = scCfgManager.scCfg
    val appCfg: StateFlow<AppCfg> = appCfgManager.appCfg

    val isScCfgPersisted: StateFlow<Boolean> = scCfgManager.isPersisted
    val isAppCfgPersisted: StateFlow<Boolean> = appCfgManager.isPersisted


    fun onEvent(event: SettingsViewModelEvent) {
        when (event) {
            is SettingsViewModelEvent.BrightnessChangedEvent -> {
                scCfgManager.setBrightnessLevel(event.level.toInt())
            }
            is SettingsViewModelEvent.ShowScoreChangedEvent -> {
                scCfgManager.setUseScore(event.isOn)
            }
            is SettingsViewModelEvent.ShowTimeChangedEvent -> {
                scCfgManager.setUseTime(event.isOn)
            }
            is SettingsViewModelEvent.AutoConnectOnStartupChangedEvent -> {
                appCfgManager.setAutoConnectOnStart(event.isOn)
            }
            is SettingsViewModelEvent.TextViewBehaviourChangedEvent -> {
                scCfgManager.setScroll(event.behaviour == TextViewBehaviour.SCROLL)
            }
            is SettingsViewModelEvent.PersistScSettingsEvent -> {
                scCfgManager.persistScCfg()
            }
            is SettingsViewModelEvent.PersistAppCfgEvent -> {
                appCfgManager.persistAppCfg()
            }
            SettingsViewModelEvent.LoadScoreCounterCfgEvent -> {
                scCfgManager.loadScCfg()
            }
        }
    }

    sealed interface SettingsViewModelEvent {
        data class BrightnessChangedEvent(val level: Float) : SettingsViewModelEvent
        data class ShowScoreChangedEvent(override var isOn: Boolean) : SettingsViewModelSwitchEvent(isOn)
        data class ShowTimeChangedEvent(override var isOn: Boolean) : SettingsViewModelSwitchEvent(isOn)
        data class AutoConnectOnStartupChangedEvent(override var isOn: Boolean) : SettingsViewModelSwitchEvent(isOn)
        data class TextViewBehaviourChangedEvent(
            val behaviour: TextViewBehaviour) : SettingsViewModelEvent
        data object PersistScSettingsEvent : SettingsViewModelEvent
        data object PersistAppCfgEvent : SettingsViewModelEvent
        data object LoadScoreCounterCfgEvent : SettingsViewModelEvent
    }

    sealed class SettingsViewModelSwitchEvent(open var isOn: Boolean) : SettingsViewModelEvent

    enum class TextViewBehaviour(val text: String) {
        ALTERNATE("Alternate Text"),
        SCROLL("Scroll Text")
    }
}