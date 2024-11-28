package com.mj.scorecounterrc

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mj.scorecounterrc.data.manager.ScoreManager
import com.mj.scorecounterrc.data.model.ScoreSide
import com.mj.scorecounterrc.viewmodel.ScoreCounterViewModel
import com.mj.scorecounterrc.ui.theme.CancelButtonContainerClr
import com.mj.scorecounterrc.ui.theme.DecrementButtonContainerClr
import com.mj.scorecounterrc.ui.theme.IncrementButtonContainerClr
import com.mj.scorecounterrc.ui.theme.OkButtonContainerClr
import com.mj.scorecounterrc.ui.theme.ResetButtonContainerClr
import com.mj.scorecounterrc.ui.theme.RotateButtonContainerClr
import com.mj.scorecounterrc.ui.theme.ScoreCounterRCTheme
import com.mj.scorecounterrc.ui.theme.SwapButtonContainerClr
import com.mj.scorecounterrc.ui.theme.TopAppBarContainerClr
import com.mj.scorecounterrc.viewmodel.ScoreCounterEvent
import timber.log.Timber

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

@Composable
fun ScoreCounter(isScFacingDown: State<Boolean>, toggleSpecialButtons: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (isScFacingDown.value) {
            ScoreCounterBack()
        }

        Row {
            val scOrientationIconPainter: Painter
            val orientationContentDesc: String

            if (isScFacingDown.value) {
                orientationContentDesc = "SC facing down"
                scOrientationIconPainter = painterResource(
                    id = R.drawable.double_arrow_down)
            } else {
                orientationContentDesc = "SC facing up"
                scOrientationIconPainter = painterResource(
                    id = R.drawable.double_arrow_up)
            }

            Box(
                modifier = Modifier
                    .size(50.dp, 94.dp)
                    .wrapContentHeight(align = Alignment.CenterVertically)
            ) {
                Icon(
                    modifier = Modifier
                        .size(50.dp)
                        .wrapContentHeight(align = Alignment.CenterVertically)
                    ,
                    painter = scOrientationIconPainter,
                    contentDescription = orientationContentDesc
                )
            }

            ScoreCounterText(ScoreSide.LEFT, toggleSpecialButtons)
            ScoreCounterText(ScoreSide.RIGHT, toggleSpecialButtons)

            Box(
                modifier = Modifier
                    .size(50.dp, 94.dp)
                    .wrapContentHeight(align = Alignment.CenterVertically)
            ) {
                Icon(
                    modifier = Modifier
                        .size(50.dp)
                        .wrapContentHeight(align = Alignment.CenterVertically)
                    ,
                    painter = scOrientationIconPainter,
                    contentDescription = orientationContentDesc
                )
            }
        }

        if (!isScFacingDown.value) {
            ScoreCounterBack()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ScoreCounterText(scoreSide: ScoreSide, toggleSpecialButtons: () -> Unit) {
    val score by ScoreManager.localScore.collectAsStateWithLifecycle()

    Text(
        modifier = Modifier
            .size(110.dp, 94.dp)
            .border(border = BorderStroke(4.dp, Color.Black))
            .wrapContentHeight(align = Alignment.CenterVertically)
            .combinedClickable(
                onLongClickLabel = "Show special buttons",
                onLongClick = {
                    toggleSpecialButtons()
                },
                onClick = {}
            ),
        text = if (scoreSide == ScoreSide.LEFT) score.left.toString()
            else score.right.toString(),
        textAlign = TextAlign.Center,
        fontSize = 38.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Serif
    )
}

@Composable
fun ScoreCounterBack() {
    HorizontalDivider(
        color = Color.Black,
        thickness = 10.dp,
        modifier = Modifier.size(330.dp, 10.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScRcTopAppBar() {
    TopAppBar(
        title = { Text(text = "Score Counter RC") },
        actions = {
            IconButton(onClick = { /*TODO*/ }) {
                Icon(
                    painter = painterResource(id = R.drawable.bluetooth),
                    contentDescription = "Connection"
                )
            }
            IconButton(onClick = { /*TODO*/ }) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = TopAppBarContainerClr
        )
    )
}

@Composable
fun RcButtonWithText(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color,
    text: String
) {
    RcButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = containerColor,
    ) {
        Text(
            text = text,
            fontSize = 26.sp,
        )
    }
}

@Composable
fun RcButtonWithIcon(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color,
    painterResourceId: Int,
    contentDescription: String?
) {
    RcButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = containerColor,
    ) {
        Icon(
            modifier = Modifier.size(50.dp),
            painter = painterResource(id = painterResourceId),
            contentDescription = contentDescription
        )
    }
}

@Composable
fun RcButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color,
    content: @Composable() (RowScope.() -> Unit)
) {
    Button(
        onClick = onClick,
        modifier = modifier.size(80.dp),
        shape = CircleShape,
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = Color.White,
        ),
    ) {
        content()
    }
}