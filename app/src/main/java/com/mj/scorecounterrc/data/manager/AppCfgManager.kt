package com.mj.scorecounterrc.data.manager

import com.mj.scorecounterrc.data.model.AppCfg
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppCfgManager @Inject constructor(
    private val storageManager: StorageManager
) {

    private val _appCfg = MutableStateFlow(AppCfg())
    val appCfg: StateFlow<AppCfg> = _appCfg.asStateFlow()

    private val _isPersisted = MutableStateFlow(true)
    val isPersisted: StateFlow<Boolean> = _isPersisted.asStateFlow()


    fun setAppCfg(appCfg: AppCfg) {
        _appCfg.update { appCfg.copy() }
        _isPersisted.update { false }
    }

    fun setAutoConnectOnStart(autoConnectOnStart: Boolean) {
        _appCfg.update { it.copy(autoConnectOnStart = autoConnectOnStart) }
        _isPersisted.update { false }
    }

    fun loadPersistedAppCfg() {
        val autoConnectOnStart = storageManager.getAutoConnectOnStartup()
        _appCfg.value = AppCfg(autoConnectOnStart = autoConnectOnStart)
    }

    fun persistAppCfg() {
        storageManager.saveAutoConnectOnStartup(_appCfg.value.autoConnectOnStart)
        _isPersisted.update { true }
    }

}
