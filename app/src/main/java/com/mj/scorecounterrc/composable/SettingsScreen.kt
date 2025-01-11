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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import com.mj.scorecounterrc.data.model.AppCfg
import com.mj.scorecounterrc.data.model.ScoreCounterCfg
import com.mj.scorecounterrc.ui.theme.SaveSettingsButtonContainerClr
import com.mj.scorecounterrc.viewmodel.ConnectionViewModel
import com.mj.scorecounterrc.viewmodel.ConnectionViewModel.ConnectionState
import com.mj.scorecounterrc.viewmodel.SettingsViewModel
import com.mj.scorecounterrc.viewmodel.SettingsViewModel.SettingsViewModelEvent
import com.mj.scorecounterrc.viewmodel.SettingsViewModel.SettingsViewModelEvent.ShowScoreChangedEvent
import com.mj.scorecounterrc.viewmodel.SettingsViewModel.SettingsViewModelEvent.ShowTimeChangedEvent
import com.mj.scorecounterrc.viewmodel.SettingsViewModel.TextViewBehaviour

enum class TabScreen(val index: Int, val title: String) {
    ScoreCounter(0, "Score Counter"),
    AppSettings(1, "App")
}

@Composable
fun SettingsScreenRoot(navigateBack: () -> Unit) {
    val settingsViewModel = hiltViewModel<SettingsViewModel>()
    val connectionViewModel = hiltViewModel<ConnectionViewModel>()

    val loadedScSettings = settingsViewModel.loadedScSettings.collectAsStateWithLifecycle()
    val isScPersistBtnEnabled = settingsViewModel.isScPersistBtnEnabled.collectAsStateWithLifecycle()
    val connectionState = connectionViewModel.connectionState.collectAsStateWithLifecycle()
    val appCfg = settingsViewModel.appCfg.collectAsStateWithLifecycle()
    val onEvent = settingsViewModel::onEvent

    SettingsScreen(
        navigateBack,
        loadedScSettings,
        isScPersistBtnEnabled,
        connectionState,
        appCfg,
        onEvent
    )
}

@SuppressLint("UnrememberedMutableState")
@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    SettingsScreen(
        onEvent = {},
        loadedScSettings = mutableStateOf(ScoreCounterCfg()),
        isScPersistBtnEnabled = mutableStateOf(false),
        connectionState = mutableStateOf(ConnectionState.NOT_CONNECTED),
        appCfg = mutableStateOf(AppCfg()),
        navigateBack = {}
    )
}

@Composable
fun SettingsScreen(
    navigateBack: () -> Unit,
    loadedScSettings: State<ScoreCounterCfg>,
    isScPersistBtnEnabled: State<Boolean>,
    connectionState: State<ConnectionState>,
    appCfg: State<AppCfg>,
    onEvent: (event: SettingsViewModelEvent) -> Unit
) {


    var selectedTab by rememberSaveable { mutableStateOf(TabScreen.ScoreCounter) }

    Scaffold(
        topBar = {
            ScRcTopAppBarRoot(
                title = "Settings",
                currScreen = CurrentScreen.Settings,
                navigateBack = navigateBack
            )
        },
        content = { innerPadding ->
            Column(
                modifier = Modifier.padding(innerPadding),
            ) {
                TabRow(selectedTabIndex = selectedTab.index) {
                    TabScreen.entries.forEach { tabScreen ->
                        Tab(
                            selected = selectedTab == tabScreen,
                            onClick = { selectedTab = tabScreen },
                            text = { Text(
                                text = tabScreen.title,
                                fontSize = 20.sp
                            ) }
                        )
                    }
                }
                Column(
                    modifier = Modifier.padding(20.dp),
                ) {
                    when (selectedTab) {
                        TabScreen.ScoreCounter -> ScoreCounterSettings(
                            loadedScSettings = loadedScSettings,
                            isScPersistBtnEnabled = isScPersistBtnEnabled,
                            connectionState = connectionState,
                            onEvent = onEvent
                        )
                        TabScreen.AppSettings -> AppSettings(
                            appCfg = appCfg,
                            onEvent = onEvent
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun ScoreCounterSettings(
    loadedScSettings: State<ScoreCounterCfg>,
    isScPersistBtnEnabled: State<Boolean>,
    connectionState: State<ConnectionState>,
    onEvent: (event: SettingsViewModelEvent) -> Unit
) {
    val minBrightness = 0f
    val maxBrightness = 15f

    var sliderPosition by rememberSaveable(loadedScSettings.value.brightness) {
        mutableFloatStateOf(loadedScSettings.value.brightness.toFloat())
    }
    val useScore = rememberSaveable(loadedScSettings.value.useScore) {
        mutableStateOf(loadedScSettings.value.useScore)
    }
    val useTime = rememberSaveable(loadedScSettings.value.useTime) {
        mutableStateOf(loadedScSettings.value.useTime)
    }
    val selectedTextViewBehaviour = rememberSaveable(loadedScSettings.value.scroll) {
        mutableStateOf(
            if (loadedScSettings.value.scroll) TextViewBehaviour.SCROLL
            else TextViewBehaviour.ALTERNATE
        )
    }

    LaunchedEffect(true) {
        onEvent(SettingsViewModelEvent.RequestLoadScConfigEvent)
    }

    Column {
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
                        onChange = { isOn ->
                            onEvent(ShowScoreChangedEvent(isOn))
                        },
                        isChecked = useScore,
                        enabled = connectionState.value == ConnectionState.CONNECTED,
                    )
                }
                Box(
                    modifier = Modifier.height(switchHeight),
                    contentAlignment = Alignment.Center

                ) {
                    SettingsSwitch(
                        onChange = { isOn ->
                            onEvent(ShowTimeChangedEvent(isOn))
                        },
                        isChecked = useTime,
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
            modifier = Modifier
                .padding(bottom = 20.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Button(
                enabled = isScPersistBtnEnabled.value,
                onClick = {
                    val config = ScoreCounterCfg()

                    config.brightness = sliderPosition.toInt()
                    config.useScore = useScore.value
                    config.useTime = useTime.value
                    config.scroll =
                        selectedTextViewBehaviour.value == TextViewBehaviour.SCROLL

                    onEvent(SettingsViewModelEvent.PersistScSettingsEvent(config))
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

@Composable
fun AppSettings(
    appCfg: State<AppCfg>,
    onEvent: (event: SettingsViewModelEvent) -> Unit = {},
) {
    val isAutoConnectOn = rememberSaveable {
        mutableStateOf(appCfg.value.autoConnectOnStart)
    }

    var isPersistBtnEnabled by rememberSaveable { mutableStateOf(false) }

    Column {
        Text(text = "Auto-connect to Score Counter when app starts")
        Spacer(modifier = Modifier.size(10.dp))
        SettingsSwitch(
            onChange = { isOn ->
                onEvent(SettingsViewModelEvent.AutoConnectOnStartupChangedEvent(isOn))
                isPersistBtnEnabled = true
            },
            isChecked = isAutoConnectOn,
            enabled = true,
        )

        Spacer(modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier
                .padding(bottom = 20.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Button(
                enabled = isPersistBtnEnabled,
                onClick = {
                    onEvent(SettingsViewModelEvent.PersistAppCfgEvent)
                    isPersistBtnEnabled = false
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