package com.mj.scorecounterrc.activity

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mj.scorecounterrc.ScoreSync
import com.mj.scorecounterrc.communication.scorecounter.ScoreCounterConnectionManager
import com.mj.scorecounterrc.communication.smartwatch.manager.SmartwatchManager
import com.mj.scorecounterrc.composable.MainScreenRoot
import com.mj.scorecounterrc.composable.SettingsScreenRoot
import com.mj.scorecounterrc.data.manager.AppCfgManager
import com.mj.scorecounterrc.ui.theme.ScoreCounterRCTheme
import com.mj.scorecounterrc.util.BluetoothRequest
import com.mj.scorecounterrc.viewmodel.EnableRequestSharedViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.Serializable
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
    @Inject
    lateinit var appCfgManager: AppCfgManager


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
                val navController = rememberNavController()
                NavHost(navController, startDestination = Main) {
                    composable<Main> {
                        MainScreenRoot(
                            onNavigateToSettings = {
                                navController.navigate(Settings)
                            }
                        )
                    }
                    composable<Settings> {
                        SettingsScreenRoot(
                            navigateBack = {
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }
        }

        if (savedInstanceState == null) {
            performInitialSetup()
        }
    }

    private fun performInitialSetup() {
        scoreCounterConnectionManager.manuallyDisconnected = false

        appCfgManager.loadPersistedAppCfg()

        if (appCfgManager.appCfg.value.autoConnectOnStart &&
            !scoreCounterConnectionManager.isBleScoreCounterConnected()) {
            Timber.i("Connecting to persisted device...")
            scoreCounterConnectionManager.startConnectionToPersistedDeviceCoroutine()
        }

        smartwatchManager.startSmartwatchApp()

        scoreSync.trySync()
    }
}

@Serializable
object Main
@Serializable
object Settings

