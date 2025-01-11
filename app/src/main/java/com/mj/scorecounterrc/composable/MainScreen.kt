package com.mj.scorecounterrc.composable

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mj.scorecounterrc.R
import com.mj.scorecounterrc.ui.theme.CancelButtonContainerClr
import com.mj.scorecounterrc.ui.theme.DecrementButtonContainerClr
import com.mj.scorecounterrc.ui.theme.IncrementButtonContainerClr
import com.mj.scorecounterrc.ui.theme.OkButtonContainerClr
import com.mj.scorecounterrc.ui.theme.ResetButtonContainerClr
import com.mj.scorecounterrc.ui.theme.RotateButtonContainerClr
import com.mj.scorecounterrc.ui.theme.ScoreCounterRCTheme
import com.mj.scorecounterrc.ui.theme.SwapButtonContainerClr
import com.mj.scorecounterrc.viewmodel.ScoreCounterEvent
import com.mj.scorecounterrc.viewmodel.ScoreCounterState
import com.mj.scorecounterrc.viewmodel.ScoreCounterViewModel

@SuppressLint("UnrememberedMutableState")
@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    ScoreCounterRCTheme {
        MainScreen(
            onNavigateToSettings = {},
            isScFacingDown = mutableStateOf(false),
            scoreCounterState = mutableStateOf(ScoreCounterState.IDLE),
            onEvent = {}
        )
    }
}

@Composable
fun MainScreenRoot(onNavigateToSettings: () -> Unit) {
    val scoreCounterViewModel = hiltViewModel<ScoreCounterViewModel>()
    val isScFacingDown = scoreCounterViewModel.isScOppositeToTheReferee.collectAsState()
    val scoreCounterState = scoreCounterViewModel.scoreCounterState.collectAsStateWithLifecycle()
    val onEvent = scoreCounterViewModel::onEvent

    MainScreen(onNavigateToSettings, isScFacingDown, scoreCounterState, onEvent)
}

@Composable
fun MainScreen(
    onNavigateToSettings: (() -> Unit),
    isScFacingDown: State<Boolean>,
    scoreCounterState: State<ScoreCounterState>,
    onEvent: (event: ScoreCounterEvent) -> Unit
) {
    var areSpecialButtonsVisible by remember { mutableStateOf(false) }
    var showCloseAppDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            ScRcTopAppBarRoot(
                title = "Remote Control",
                currScreen = CurrentScreen.Main,
                onNavigateToSettings = onNavigateToSettings
            )
        },
        content = { innerPadding ->
            Column(
                modifier = Modifier.padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.weight(1f))
                RcButtonWithIcon(
                    onClick = {
                        if (areSpecialButtonsVisible) {
                            onEvent(ScoreCounterEvent.ToggleOrientation)
                        }
                    },
                    modifier = Modifier
                        .rotate(270f)
                        .alpha(if (areSpecialButtonsVisible) 1f else 0f),
                    containerColor = RotateButtonContainerClr,
                    painterResourceId = R.drawable.rotate,
                    contentDescription = "Rotate Score Counter"
                )
                Spacer(modifier = Modifier.size(50.dp))
                RcButtonWithIcon(
                    onClick = {
                        if (areSpecialButtonsVisible) {
                            onEvent(ScoreCounterEvent.SwapScore)
                        }
                    },
                    modifier = Modifier.alpha(if (areSpecialButtonsVisible) 1f else 0f),
                    containerColor = SwapButtonContainerClr,
                    painterResourceId = R.drawable.swap,
                    contentDescription = "Swap score"
                )
                Spacer(modifier = Modifier.size(50.dp))

                Row {
                    RcButtonWithIcon(
                        onClick = {
                            if (scoreCounterState.value != ScoreCounterState.IDLE) {
                                onEvent(ScoreCounterEvent.CancelButtonClicked)
                            }
                        },
                        modifier = Modifier
                            .alpha(
                                if (scoreCounterState.value != ScoreCounterState.IDLE) 1f else 0f),
                        containerColor = CancelButtonContainerClr,
                        painterResourceId = R.drawable.cancel,
                        contentDescription = "Cancel"
                    )
                    Spacer(modifier = Modifier.size(30.dp))
                    RcButtonWithText(
                        onClick = {
                            if (areSpecialButtonsVisible) {
                                onEvent(ScoreCounterEvent.ResetButtonClicked)
                            }
                        },
                        modifier = Modifier.alpha(if (areSpecialButtonsVisible) 1f else 0f),
                        containerColor = ResetButtonContainerClr,
                        text = "0:0",
                    )
                    Spacer(modifier = Modifier.size(30.dp))
                    RcButtonWithIcon(
                        onClick = {
                            if (scoreCounterState.value != ScoreCounterState.IDLE) {
                                onEvent(ScoreCounterEvent.OkButtonClicked)
                            }
                        },
                        modifier = Modifier
                            .alpha(
                                if (scoreCounterState.value != ScoreCounterState.IDLE) 1f else 0f),
                        containerColor = OkButtonContainerClr,
                        painterResourceId = R.drawable.check,
                        contentDescription = "OK"
                    )
                }

                Spacer(modifier = Modifier.size(60.dp))

                Row {
                    Spacer(modifier = Modifier.weight(1f))
                    RcButtonWithText(
                        onClick = {
                            onEvent(ScoreCounterEvent.IncrementLeftScore)
                        },
                        containerColor = IncrementButtonContainerClr,
                        text = "+1",
                    )
                    Spacer(modifier = Modifier.size(20.dp))
                    RcButtonWithText(
                        onClick = {
                            onEvent(ScoreCounterEvent.IncrementRightScore)
                        },
                        containerColor = IncrementButtonContainerClr,
                        text = "+1",
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.size(20.dp))
                ScoreCounter(
                    isScFacingDown = isScFacingDown,
                    toggleSpecialButtons = {
                        areSpecialButtonsVisible = !areSpecialButtonsVisible
                    })
                Spacer(modifier = Modifier.size(20.dp))

                Row {
                    Spacer(modifier = Modifier.weight(1f))
                    RcButtonWithText(
                        onClick = {
                            onEvent(ScoreCounterEvent.DecrementLeftScore)
                        },
                        containerColor = DecrementButtonContainerClr,
                        text = "-1",
                    )
                    Spacer(modifier = Modifier.size(20.dp))
                    RcButtonWithText(
                        onClick = {
                            onEvent(ScoreCounterEvent.DecrementRightScore)
                        },
                        containerColor = DecrementButtonContainerClr,
                        text = "-1",
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.size(20.dp))
            }

            BackHandler {
                showCloseAppDialog = true
            }

            if (showCloseAppDialog) {
                CloseAppDialogRoot(onDismiss = { showCloseAppDialog = false })
            }
        }
    )
}
