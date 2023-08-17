package com.metamask.dapp

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DappLabel(
    text: String,
    color: Color = Color.White,
    fontSize: TextUnit = 12.sp,
    modifier: Modifier = Modifier.padding(bottom = 12.dp)
) {
    Text(
        text = text,
        color = color,
        fontSize = fontSize,
        modifier = modifier
    )
}

@Preview
@Composable
fun PreviewDappLabel() {
    DappLabel(text = "Connect")
}