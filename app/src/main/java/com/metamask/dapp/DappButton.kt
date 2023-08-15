package com.metamask.dapp

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DappButton(
    buttonText: String,
    buttonHeight: Dp = 48.dp,
    buttonBackgroundColor: Color = Color.Blue,
    buttonTextColor: Color = Color.White,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        elevation = null, colors = ButtonDefaults.buttonColors(
            containerColor = buttonBackgroundColor,
            contentColor = buttonTextColor
        ), modifier = Modifier
            .height(buttonHeight)
            .fillMaxWidth()
    ) {
        Text(
            text = buttonText,
            fontSize = 18.sp,
            color = Color.White,
        )
    }
}

@Preview
@Composable
fun PreviewButtonWithCaption() {
    DappButton(
        buttonText = "Connect",
        onClick = {}
    )
}