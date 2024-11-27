package com.mj.scorecounterrc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mj.scorecounterrc.data.manager.ScoreManager
import com.mj.blescorecounterremotecontroller.viewmodel.DisplayViewModel
import com.mj.scorecounterrc.ui.theme.CancelButtonContainerClr
import com.mj.scorecounterrc.ui.theme.DecrementButtonContainerClr
import com.mj.scorecounterrc.ui.theme.IncrementButtonContainerClr
import com.mj.scorecounterrc.ui.theme.OkButtonContainerClr
import com.mj.scorecounterrc.ui.theme.ResetButtonContainerClr
import com.mj.scorecounterrc.ui.theme.RotateButtonContainerClr
import com.mj.scorecounterrc.ui.theme.ScoreCounterRCTheme
import com.mj.scorecounterrc.ui.theme.SwapButtonContainerClr
import com.mj.scorecounterrc.ui.theme.TopAppBarContainerClr

class MainActivity : ComponentActivity() {

    private val displayViewModel: DisplayViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ScoreCounterRCTheme {
                MainScreen(displayViewModel)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    val displayViewModel = DisplayViewModel()

    ScoreCounterRCTheme {
        MainScreen(displayViewModel)
    }
}

@Composable
fun MainScreen(displayViewModel: DisplayViewModel) {
    val isScFacingDown by displayViewModel.isFacingToTheReferee.collectAsState()
    val score by ScoreManager.localScore.collectAsState()

    Scaffold(
        topBar = {
            ScRcTopAppBar()
        },
        content = { innerPadding ->
            Column(
                modifier = Modifier.padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally
//                contentAlignment = Alignment.BottomCenter
            ) {
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = { /*TODO*/ },
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RotateButtonContainerClr,
                        contentColor = Color.White,
                    ),
                ) {
                    Icon(
                        modifier = Modifier.size(50.dp),
                        painter = painterResource(id = R.drawable.rotate),
                        contentDescription = "Rotate Score Counter"
                    )
                }
                Spacer(modifier = Modifier.size(50.dp))
                Button(
                    onClick = { /*TODO*/ },
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SwapButtonContainerClr,
                        contentColor = Color.White,
                    ),
                ) {
                    Icon(
                        modifier = Modifier.size(50.dp),
                        painter = painterResource(id = R.drawable.swap),
                        contentDescription = "Swap"
                    )
                }
                Spacer(modifier = Modifier.size(50.dp))
                Row {
                    Button(
                        onClick = { /*TODO*/ },
                        modifier = Modifier.size(80.dp),
                        shape = CircleShape,
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CancelButtonContainerClr,
                            contentColor = Color.White,
                        ),
                    ) {
                        Icon(
                            modifier = Modifier.size(50.dp),
                            painter = painterResource(id = R.drawable.cancel),
                            contentDescription = "Cancel"
                        )
                    }
                    Spacer(modifier = Modifier.size(30.dp))
                    Button(
                        onClick = { /*TODO*/ },
                        modifier = Modifier.size(80.dp),
                        shape = CircleShape,
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ResetButtonContainerClr,
                            contentColor = Color.White,
                        ),
                    ) {
                        Text(
                            text = "0:0",
                            fontSize = 26.sp,
                        )
                    }
                    Spacer(modifier = Modifier.size(30.dp))
                    Button(
                        onClick = { /*TODO*/ },
                        modifier = Modifier.size(80.dp),
                        shape = CircleShape,
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OkButtonContainerClr,
                            contentColor = Color.White,
                        ),
                    ) {
                        Icon(
                            modifier = Modifier.size(50.dp),
                            painter = painterResource(id = R.drawable.check),
                            contentDescription = "OK"
                        )
                    }
                }
                Spacer(modifier = Modifier.size(60.dp))
                Row {
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = { /*TODO*/ },
                        modifier = Modifier.size(80.dp),
                        shape = CircleShape,
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = IncrementButtonContainerClr,
                            contentColor = Color.White,
                        ),
                    ) {
                        Text(
                            text = "+1",
                            fontSize = 26.sp,
                        )
                    }
                    Spacer(modifier = Modifier.size(20.dp))
                    Button(
                        onClick = { /*TODO*/ },
                        modifier = Modifier.size(80.dp),
                        shape = CircleShape,
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = IncrementButtonContainerClr,
                            contentColor = Color.White,
                        ),
                    ) {
                        Text(
                            text = "+1",
                            fontSize = 26.sp,
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.size(20.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (isScFacingDown) {
                        HorizontalDivider(
                            color = Color.Black,
                            thickness = 10.dp,
                            modifier = Modifier.size(330.dp, 10.dp)
                        )
                    }

                    Row {
                        val scOrientationIconPainter: Painter
                        val orientationContentDesc: String

                        if (isScFacingDown) {
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
                        Text(
                            modifier = Modifier
                                .size(110.dp, 94.dp)
                                .border(border = BorderStroke(4.dp, Color.Black))
                                .wrapContentHeight(align = Alignment.CenterVertically),
                            text = score.left.toString(),
                            textAlign = TextAlign.Center,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif
                        )
                        Text(
                            modifier = Modifier
                                .size(110.dp, 94.dp)
                                .border(border = BorderStroke(4.dp, Color.Black))
                                .wrapContentHeight(align = Alignment.CenterVertically),
                            text = score.right.toString(),
                            textAlign = TextAlign.Center,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif
                        )
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
                                painter = painterResource(id = R.drawable.double_arrow_up),
                                contentDescription = orientationContentDesc
                            )
                        }
                    }
                    if (!isScFacingDown) {
                        HorizontalDivider(
                            color = Color.Black,
                            thickness = 10.dp,
                            modifier = Modifier.size(330.dp, 10.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.size(20.dp))
                Row(
//                    verticalAlignment = Alignment.Bottom,
//                    horizontalArrangement = Arrangement.Center
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = { /*TODO*/ },
                        modifier = Modifier.size(80.dp),
                        shape = CircleShape,
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DecrementButtonContainerClr,
                            contentColor = Color.White,
                        ),
                    ) {
                        Text(
                            text = "-1",
                            fontSize = 26.sp,
                        )
                    }
                    Spacer(modifier = Modifier.size(20.dp))
                    Button(
                        onClick = { /*TODO*/ },
                        modifier = Modifier.size(80.dp),
                        shape = CircleShape,
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DecrementButtonContainerClr,
                            contentColor = Color.White,
                        ),
                    ) {
                        Text(
                            text = "-1",
                            fontSize = 26.sp,
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.size(20.dp))
            }
        }
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
                Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = TopAppBarContainerClr
        )
    )
}