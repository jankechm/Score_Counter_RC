package com.mj.scorecounterrc.composable

import android.annotation.SuppressLint
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mj.scorecounterrc.R
import com.mj.scorecounterrc.data.manager.ScoreManager
import com.mj.scorecounterrc.ui.theme.CancelButtonContainerClr
import com.mj.scorecounterrc.ui.theme.DecrementButtonContainerClr
import com.mj.scorecounterrc.ui.theme.IncrementButtonContainerClr
import com.mj.scorecounterrc.ui.theme.OkButtonContainerClr
import com.mj.scorecounterrc.ui.theme.ResetButtonContainerClr
import com.mj.scorecounterrc.ui.theme.RotateButtonContainerClr
import com.mj.scorecounterrc.ui.theme.ScoreCounterRCTheme
import com.mj.scorecounterrc.ui.theme.SwapButtonContainerClr
import com.mj.scorecounterrc.viewmodel.ScoreCounterEvent
import com.mj.scorecounterrc.viewmodel.ScoreCounterViewModel
import timber.log.Timber

@SuppressLint("UnrememberedMutableState")
@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    ScoreCounterRCTheme {
        MainScreen(isScFacingDown = mutableStateOf(false), onEvent = {})
    }
}

@Composable
fun MainScreenRoot(scoreCounterViewModel: ScoreCounterViewModel) {
    val isScFacingDown = scoreCounterViewModel.isScFacingToTheReferee.collectAsState()
    val onEvent = scoreCounterViewModel::onEvent

    MainScreen(isScFacingDown, onEvent)
}

@Composable
fun MainScreen(isScFacingDown: State<Boolean>, onEvent: (event: ScoreCounterEvent) -> Unit) {
    var areOkAndCancelButtonsVisible by rememberSaveable { mutableStateOf(false) }
    var areSpecialButtonsVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            ScRcTopAppBar()
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
                            areOkAndCancelButtonsVisible = true
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
                            ScoreManager.swapScore()
                            areOkAndCancelButtonsVisible = true
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
                            if (areOkAndCancelButtonsVisible) {
                                onEvent(ScoreCounterEvent.RevertOrientation)
                                ScoreManager.revertScore()
                                areOkAndCancelButtonsVisible = false
                            }
                        },
                        modifier = Modifier.alpha(if (areOkAndCancelButtonsVisible) 1f else 0f),
                        containerColor = CancelButtonContainerClr,
                        painterResourceId = R.drawable.cancel,
                        contentDescription = "Cancel"
                    )
                    Spacer(modifier = Modifier.size(30.dp))
                    RcButtonWithText(
                        onClick = {
                            if (areSpecialButtonsVisible) {
                                ScoreManager.resetScore()
                                areOkAndCancelButtonsVisible = true
                            }
                        },
                        modifier = Modifier.alpha(if (areSpecialButtonsVisible) 1f else 0f),
                        containerColor = ResetButtonContainerClr,
                        text = "0:0",
                    )
                    Spacer(modifier = Modifier.size(30.dp))
                    RcButtonWithIcon(
                        onClick = {
                            if (areOkAndCancelButtonsVisible) {
                                // TODO send score to SC and watch
                                ScoreManager.confirmNewScore(true)
                                // TODO hide OK and Cancel button in offline mode
                                onEvent(ScoreCounterEvent.ConfirmOrientation)

                                Timber.d("Local score timestamp: ${ScoreManager.timestamp}")

                                areOkAndCancelButtonsVisible = false
                                areSpecialButtonsVisible = false

                                // TODO persist score
                            }
                        },
                        modifier = Modifier.alpha(if (areOkAndCancelButtonsVisible) 1f else 0f),
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
                            ScoreManager.incrementLeftScore()
                            areOkAndCancelButtonsVisible = true
                        },
                        containerColor = IncrementButtonContainerClr,
                        text = "+1",
                    )
                    Spacer(modifier = Modifier.size(20.dp))
                    RcButtonWithText(
                        onClick = {
                            ScoreManager.incrementRightScore()
                            areOkAndCancelButtonsVisible = true
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
                            ScoreManager.decrementLeftScore()
                            areOkAndCancelButtonsVisible = true
                        },
                        containerColor = DecrementButtonContainerClr,
                        text = "-1",
                    )
                    Spacer(modifier = Modifier.size(20.dp))
                    RcButtonWithText(
                        onClick = {
                            ScoreManager.decrementRightScore()
                            areOkAndCancelButtonsVisible = true
                        },
                        containerColor = DecrementButtonContainerClr,
                        text = "-1",
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.size(20.dp))
            }
        }
    )
}