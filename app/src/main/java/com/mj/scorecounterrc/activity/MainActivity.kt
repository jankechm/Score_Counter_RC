package com.mj.scorecounterrc.activity

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.mj.scorecounterrc.composable.MainScreenRoot
import com.mj.scorecounterrc.ui.theme.ScoreCounterRCTheme
import com.mj.scorecounterrc.util.BluetoothRequest
import com.mj.scorecounterrc.viewmodel.EnableRequestSharedViewModel
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val enableRequestSharedViewModel: EnableRequestSharedViewModel by viewModels()

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
        enableRequestSharedViewModel.requestEnableBluetoothCallback = {
            enableBluetoothLauncher.launch(Unit)
        }

        super.onCreate(savedInstanceState)
        setContent {
            ScoreCounterRCTheme {
                MainScreenRoot()
            }
        }
    }
}


