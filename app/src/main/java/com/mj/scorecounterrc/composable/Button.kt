package com.mj.scorecounterrc.composable

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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