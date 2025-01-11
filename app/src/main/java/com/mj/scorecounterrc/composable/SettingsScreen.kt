package com.mj.scorecounterrc.composable

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mj.scorecounterrc.data.model.ScoreCounterCfg
import com.mj.scorecounterrc.ui.theme.SaveSettingsButtonContainerClr
import com.mj.scorecounterrc.viewmodel.ConnectionViewModel
import com.mj.scorecounterrc.viewmodel.ConnectionViewModel.ConnectionState
import com.mj.scorecounterrc.viewmodel.SettingsViewModel
import com.mj.scorecounterrc.viewmodel.SettingsViewModel.SettingsViewModelEvent
import com.mj.scorecounterrc.viewmodel.SettingsViewModel.TextViewBehaviour

@Composable
fun SettingsScreenRoot(navigateBack: () -> Unit) {
    val settingsViewModel = hiltViewModel<SettingsViewModel>()
    val connectionViewModel = hiltViewModel<ConnectionViewModel>()

    val loadedSettings = settingsViewModel.loadedSettings.collectAsStateWithLifecycle()
    val isPersistBtnEnabled = settingsViewModel.isPersistBtnEnabled.collectAsStateWithLifecycle()
    val connectionState = connectionViewModel.connectionState.collectAsStateWithLifecycle()
    val onEvent = settingsViewModel::onEvent

    SettingsScreen(navigateBack, loadedSettings, isPersistBtnEnabled, connectionState, onEvent)
}

@SuppressLint("UnrememberedMutableState")
@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    SettingsScreen(
        onEvent = {},
        loadedSettings = mutableStateOf(ScoreCounterCfg()),
        isPersistBtnEnabled = mutableStateOf(false),
        connectionState = mutableStateOf(ConnectionState.NOT_CONNECTED),
        navigateBack = {}
    )
}

@Composable
fun SettingsScreen(
    navigateBack: () -> Unit,
    loadedSettings: State<ScoreCounterCfg>,
    isPersistBtnEnabled: State<Boolean>,
    connectionState: State<ConnectionState>,
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
                        enabled = connectionState.value == ConnectionState.CONNECTED,
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
                        val switchHeight = 70.dp
                        Column {
                            Box(
                                modifier = Modifier.height(switchHeight),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    textAlign = TextAlign.Center,
                                    text = "Show Score"
                                )
                            }
                            Box(
                                modifier = Modifier.height(switchHeight),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    textAlign = TextAlign.Center,
                                    text = "Show Time"
                                )
                            }
                        }
                        Spacer(modifier = Modifier.size(10.dp))
                        Column {
                            Box(
                                modifier = Modifier.height(switchHeight),
                                contentAlignment = Alignment.Center

                            ) {
                                SettingsSwitch(
                                    onEvent = onEvent,
                                    isChecked = useScore,
                                    switchType = SwitchType.SHOW_SCORE,
                                    enabled = connectionState.value == ConnectionState.CONNECTED,
                                )
                            }
                            Box(
                                modifier = Modifier.height(switchHeight),
                                contentAlignment = Alignment.Center

                            ) {
                                SettingsSwitch(
                                    onEvent = onEvent,
                                    isChecked = useTime,
                                    switchType = SwitchType.SHOW_TIME,
                                    enabled = connectionState.value == ConnectionState.CONNECTED,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.size(40.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SettingsRadioRow(
                            onEvent = onEvent,
                            selectedTextViewBehaviour = selectedTextViewBehaviour,
                            textViewBehaviour = TextViewBehaviour.ALTERNATE,
                            enabled = connectionState.value == ConnectionState.CONNECTED,
                        )
                        Spacer(modifier = Modifier.size(20.dp))
                        SettingsRadioRow(
                            onEvent = onEvent,
                            selectedTextViewBehaviour = selectedTextViewBehaviour,
                            textViewBehaviour = TextViewBehaviour.SCROLL,
                            enabled = connectionState.value == ConnectionState.CONNECTED,
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Box(
                        modifier = Modifier.padding(bottom = 20.dp)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            enabled = isPersistBtnEnabled.value,
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
                            Text(
                                text = "Persist",
                                fontSize = 20.sp
                            )
                        }
                    }
                }
            }
        }
    )
}

