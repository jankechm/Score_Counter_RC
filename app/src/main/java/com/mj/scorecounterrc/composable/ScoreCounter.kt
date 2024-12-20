package com.mj.scorecounterrc.composable

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mj.scorecounterrc.R
import com.mj.scorecounterrc.data.manager.ScoreManager
import com.mj.scorecounterrc.data.model.ScoreSide

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ScoreCounter(isScFacingDown: State<Boolean>, toggleSpecialButtons: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (isScFacingDown.value) {
            ScoreCounterBack()
        }

        Row(
            modifier = Modifier.combinedClickable(
                onLongClickLabel = "Show special buttons",
                onLongClick = {
                    toggleSpecialButtons()
                },
                onClick = {}
            ),
        ) {
            val scOrientationIconPainter: Painter
            val orientationContentDesc: String

            if (isScFacingDown.value) {
                orientationContentDesc = "SC facing down"
                scOrientationIconPainter = painterResource(
                    id = R.drawable.double_arrow_down
                )
            } else {
                orientationContentDesc = "SC facing up"
                scOrientationIconPainter = painterResource(
                    id = R.drawable.double_arrow_up
                )
            }

            Box(
                modifier = Modifier
                    .size(50.dp, 94.dp)
                    .wrapContentHeight(align = Alignment.CenterVertically)
            ) {
                Icon(
                    modifier = Modifier
                        .size(50.dp)
                        .wrapContentHeight(align = Alignment.CenterVertically),
                    painter = scOrientationIconPainter,
                    contentDescription = orientationContentDesc
                )
            }

            ScoreCounterText(ScoreSide.LEFT)
            ScoreCounterText(ScoreSide.RIGHT)

            Box(
                modifier = Modifier
                    .size(50.dp, 94.dp)
                    .wrapContentHeight(align = Alignment.CenterVertically)
            ) {
                Icon(
                    modifier = Modifier
                        .size(50.dp)
                        .wrapContentHeight(align = Alignment.CenterVertically),
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

@Composable
fun ScoreCounterText(scoreSide: ScoreSide) {
    val score by ScoreManager.localScore.collectAsStateWithLifecycle()

    Text(
        modifier = Modifier
            .size(110.dp, 94.dp)
            .border(border = BorderStroke(4.dp, Color.Black))
            .wrapContentHeight(align = Alignment.CenterVertically),
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