package com.metamask.dapp

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.metamask.dapp.com.metamask.dapp.AppTopBar
import io.metamask.androidsdk.EthereumState
import io.metamask.androidsdk.Result
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SendTransactionScreen(
    navController: NavController,
    ethereumState: EthereumState,
    isConnectWith: Boolean = false,
    sendTransaction: suspend (value: String, from: String, to: String) -> Result<String>,
    connectWithSendTransaction: suspend (value: String, from: String, to: String) -> Result<String>
) {
    var value by remember { mutableStateOf("0x8ac7230489e80000") }
    var from by remember { mutableStateOf(ethereumState.selectedAddress) }
    var to by remember { mutableStateOf("0x0000000000000000000000000000000000000000") }
    var sendResult by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    var errorMessage by remember { mutableStateOf<String?>(null) }

    val keyboardController = LocalSoftwareKeyboardController.current

    Surface {
        AppTopBar(navController)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Heading("Send Transaction")

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .height(48.dp)
            ) {
                Text(
                    text = "Value:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .width(72.dp)
                        .padding(end = 16.dp)
                        .align(Alignment.CenterVertically)
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .border(BorderStroke(1.dp, Color.Gray), shape = RoundedCornerShape(10.dp))
                        .clickable {
                            keyboardController?.hide()
                        }
                ) {
                    BasicTextField(
                        value = value,
                        textStyle = TextStyle(color = if (isSystemInDarkTheme()) { Color.White} else { Color.Black}),
                        onValueChange = {
                            value = it
                        },
                        modifier = Modifier
                            .padding(start = 8.dp, top = 16.dp, end = 8.dp, bottom = 0.dp)
                            .fillMaxSize()
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .height(48.dp)
            ) {

                Text(
                    text = "From:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .width(72.dp)
                        .padding(end = 16.dp)
                        .align(Alignment.CenterVertically)
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .border(BorderStroke(1.dp, Color.Gray), shape = RoundedCornerShape(10.dp))
                        .clickable {
                            keyboardController?.hide()
                        }
                ) {
                    BasicTextField(
                        value = from,
                        textStyle = TextStyle(color = if (isSystemInDarkTheme()) { Color.White} else { Color.Black}),
                        onValueChange = {
                            from = it
                        },
                        modifier = Modifier
                            .padding(start = 8.dp, top = 16.dp, end = 8.dp, bottom = 0.dp)
                            .fillMaxSize()
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .height(48.dp)
            ) {

                Text(
                    text = "To:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .width(72.dp)
                        .padding(end = 16.dp)
                        .align(Alignment.CenterVertically)
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .border(BorderStroke(1.dp, Color.Gray), shape = RoundedCornerShape(10.dp))
                        .clickable {
                            keyboardController?.hide()
                        }
                ) {
                    BasicTextField(
                        value = to,
                        textStyle = TextStyle(color = if (isSystemInDarkTheme()) { Color.White} else { Color.Black}),
                        onValueChange = {
                            to = it
                        },
                        modifier = Modifier
                            .padding(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 8.dp)
                            .fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isConnectWith) {
                DappButton(buttonText = stringResource(R.string.connect_with_send)) {
                    coroutineScope.launch {
                        when (val result = connectWithSendTransaction(value, from, to)) {
                            is Result.Success<String> -> {
                                errorMessage = null
                                sendResult = result.value
                            }
                            is Result.Error -> {
                                errorMessage = result.error.message
                            }
                            else -> {}
                        }
                    }
                }
            } else {
                DappButton(buttonText = stringResource(R.string.send)) {
                    coroutineScope.launch {
                        when (val result = sendTransaction(value, from, to)) {
                            is Result.Success -> {
                                errorMessage = null
                                sendResult = result.value
                            }
                            is Result.Error -> {
                                errorMessage = result.error.message
                            }
                            else -> {}
                        }
                    }
                }
            }

            DappLabel(
                text =  errorMessage ?: sendResult,
                color = if (errorMessage != null) { Color.Red } else { Color.Unspecified },
                modifier = Modifier.padding(bottom = 36.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Preview
@Composable
fun PreviewSendTransaction() {
    SendTransactionScreen(
        rememberNavController(),
        ethereumState = EthereumState("", "", ""),
        sendTransaction = { _, _, _ -> Result.Success("") },
        connectWithSendTransaction = { _, _, _ -> Result.Success("") },
    )
}