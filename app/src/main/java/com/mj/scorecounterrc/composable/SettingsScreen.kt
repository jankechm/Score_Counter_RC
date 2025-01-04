package com.mj.scorecounterrc.composable

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mj.scorecounterrc.data.model.ScoreCounterCfg
import com.mj.scorecounterrc.ui.theme.SaveSettingsButtonContainerClr
import com.mj.scorecounterrc.viewmodel.SettingsViewModel
import com.mj.scorecounterrc.viewmodel.SettingsViewModel.SettingsViewModelEvent
import com.mj.scorecounterrc.viewmodel.SettingsViewModel.TextViewBehaviour

@Composable
fun SettingsScreenRoot(navigateBack: () -> Unit) {
    val settingsViewModel = hiltViewModel<SettingsViewModel>()
    val loadedSettings = settingsViewModel.loadedSettings.collectAsStateWithLifecycle()
    val onEvent = settingsViewModel::onEvent

    SettingsScreen(navigateBack, loadedSettings, onEvent)
}

@SuppressLint("UnrememberedMutableState")
@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    SettingsScreen(
        onEvent = {},
        loadedSettings = mutableStateOf(ScoreCounterCfg()),
        navigateBack = {}
    )
}

@Composable
fun SettingsScreen(
    navigateBack: () -> Unit,
    loadedSettings: State<ScoreCounterCfg>,
    onEvent: (event: SettingsViewModelEvent) -> Unit
) {
    val minBrightness = 0f
    val maxBrightness = 15f

    var sliderPosition by rememberSaveable(loadedSettings.value.brightness) {
        mutableFloatStateOf(loadedSettings.value.brightness.toFloat())
    }
    val useScore = rememberSaveable(loadedSettings.value.useScore) {
        mutableStateOf(loadedSettings.value.useScore)
    }
    val useTime = rememberSaveable(loadedSettings.value.useTime) {
        mutableStateOf(loadedSettings.value.useTime)
    }
    val selectedTextViewBehaviour = rememberSaveable(loadedSettings.value.scroll) {
        mutableStateOf(
            if (loadedSettings.value.scroll) TextViewBehaviour.SCROLL
            else TextViewBehaviour.ALTERNATE
        )
    }

    Scaffold(
        topBar = {
            ScRcTopAppBarRoot(CurrentScreen.Settings, navigateBack = navigateBack)
        },
        content = { innerPadding ->
            LaunchedEffect(true) {
                onEvent(SettingsViewModelEvent.RequestLoadDataEvent)
            }

            Column(
                modifier = Modifier.padding(innerPadding),
//                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                ) {
                    Text(text = "Brightness")
                    Spacer(modifier = Modifier.size(10.dp))
                    Slider(
                        value = sliderPosition,
                        onValueChange = {
                            sliderPosition = it
                            onEvent(SettingsViewModelEvent.BrightnessChangedEvent(it))
                        },
                        valueRange = minBrightness..maxBrightness,
                        steps = maxBrightness.toInt() - minBrightness.toInt() + 1
                    )

                    Spacer(modifier = Modifier.size(40.dp))

                    Row {
                        Column {
                            Text("Show Score")
                            Text("Show Time")
                        }
                        Column {
                            SettingsSwitch(onEvent, useScore, SwitchType.SHOW_SCORE)
                            SettingsSwitch(onEvent, useTime, SwitchType.SHOW_TIME)
                        }
                    }

                    Spacer(modifier = Modifier.size(40.dp))

                    Row {
                        SettingsRadioRow(
                            onEvent, selectedTextViewBehaviour, TextViewBehaviour.ALTERNATE)
                        Spacer(modifier = Modifier.size(20.dp))
                        SettingsRadioRow(
                            onEvent, selectedTextViewBehaviour, TextViewBehaviour.SCROLL)
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Button(
                        onClick = {
                            val config = ScoreCounterCfg()

                            config.brightness = sliderPosition.toInt()
                            config.useScore = useScore.value
                            config.useTime = useTime.value
                            config.scroll =
                                selectedTextViewBehaviour.value == TextViewBehaviour.SCROLL

                            onEvent(SettingsViewModelEvent.PersistSettingsEvent(config))
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SaveSettingsButtonContainerClr,
                            contentColor = Color.White,
                        ),
                    ) {
                        Text(text = "Persist")
                    }
                }
            }
        }
    )
}

