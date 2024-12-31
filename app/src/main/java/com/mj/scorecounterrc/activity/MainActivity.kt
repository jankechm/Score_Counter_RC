package com.mj.scorecounterrc.activity

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.mj.scorecounterrc.ScoreSync
import com.mj.scorecounterrc.communication.scorecounter.ScoreCounterConnectionManager
import com.mj.scorecounterrc.communication.smartwatch.manager.SmartwatchManager
import com.mj.scorecounterrc.composable.MainScreenRoot
import com.mj.scorecounterrc.data.manager.AppCfgManager
import com.mj.scorecounterrc.ui.theme.ScoreCounterRCTheme
import com.mj.scorecounterrc.util.BluetoothRequest
import com.mj.scorecounterrc.viewmodel.EnableRequestSharedViewModel
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val enableRequestSharedViewModel: EnableRequestSharedViewModel by viewModels()

    @Inject
    lateinit var scoreCounterConnectionManager: ScoreCounterConnectionManager
    @Inject
    lateinit var scoreSync: ScoreSync
    @Inject
    lateinit var smartwatchManager: SmartwatchManager


    private val enableBluetoothLauncher = registerForActivityResult(
        BluetoothRequest.EnableBluetoothContract()
    ) { isEnabled ->
        if (isEnabled) {
            Timber.i("Enable Bluetooth activity result OK")
        } else {
            Timber.i("Enable Bluetooth activity result DENIED")
            Toast.makeText(this, "Bluetooth was NOT enabled!", Toast.LENGTH_SHORT)
                .show()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableRequestSharedViewModel.requestEnableBluetoothCallback = {
            enableBluetoothLauncher.launch(Unit)
        }

        setContent {
            ScoreCounterRCTheme {
                MainScreenRoot()
            }
        }

        if (savedInstanceState == null) {
            performInitialSetup()
        }
    }

    private fun performInitialSetup() {
        scoreCounterConnectionManager.manuallyDisconnected = false

        if (AppCfgManager.appCfg.autoConnectOnStart &&
            !scoreCounterConnectionManager.isBleScoreCounterConnected()) {
            Timber.i("Connecting to persisted device...")
            scoreCounterConnectionManager.startConnectionToPersistedDeviceCoroutine()
        }

        smartwatchManager.startSmartwatchApp()

        scoreSync.trySync()
    }
}


