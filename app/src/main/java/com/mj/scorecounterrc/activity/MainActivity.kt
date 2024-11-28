package com.mj.scorecounterrc.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.mj.scorecounterrc.composable.MainScreenRoot
import com.mj.scorecounterrc.viewmodel.ScoreCounterViewModel
import com.mj.scorecounterrc.ui.theme.ScoreCounterRCTheme

class MainActivity : ComponentActivity() {

    private val scoreCounterViewModel: ScoreCounterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ScoreCounterRCTheme {
                MainScreenRoot(scoreCounterViewModel)
            }
        }
    }
}

