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
    labelPrefix: String = "",
    text: String,
    color: Color = Color.White,
    fontSize: TextUnit = 14.sp,
    modifier: Modifier = Modifier.padding(bottom = 12.dp)
) {
    if (labelPrefix.isNotEmpty()) {
        Text(
            text = labelPrefix,
            color = color,
            fontSize = fontSize.times(1.3),
            modifier = Modifier.padding(bottom = 5.dp)
        )
    }
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