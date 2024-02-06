package com.metamask.dapp

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.metamask.dapp.com.metamask.dapp.AppTopBar
import io.metamask.androidsdk.EthereumState
import io.metamask.androidsdk.Result
import kotlinx.coroutines.launch

@Composable
fun BatchSignMessageScreen(
    navController: NavController,
    ethereumState: EthereumState,
    batchSign: suspend (messages: List<String>, address: String) -> Result
) {
    var signResult by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val transactionData = "{\"data\":\"0xd46e8dd67c5d32be8d46e8dd67c5d32be8058bb8eb970870f072445675058bb8eb970870f072445675\",\"from\": \"0x0000000000000000000000000000000000000000\",\"gas\": \"0x76c0\",\"gasPrice\": \"0x9184e72a000\",\"to\": \"0xd46e8dd67c5d32be8058bb8eb970870f07244567\",\"value\": \"0x9184e72a\"}"
    val helloWorld = "Hello, world, signing in!"
    val byeWorld = "Last message to sign!"
    val batchSignMessages: List<String> = listOf(helloWorld, transactionData, byeWorld)
    val coroutineScope = rememberCoroutineScope()

    Surface {
        AppTopBar(navController)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Heading("Sign Message")

            Spacer(modifier = Modifier.weight(1f))

            BasicTextField(
                value = batchSignMessages.joinToString("\n\n=================================\n\n"),
                textStyle = TextStyle(color = if (isSystemInDarkTheme()) { Color.White} else { Color.Black}),
                onValueChange = { },
                modifier = Modifier.padding(bottom = 36.dp)
            )

            DappButton(buttonText = stringResource(R.string.batch_sign)) {
                coroutineScope.launch {
                    when (val result = batchSign(batchSignMessages, ethereumState.selectedAddress)) {
                        is Result.Success.Items -> {
                            errorMessage = null
                            signResult = result.value.joinToString("\n=================================\n")
                        }
                        is Result.Error -> {
                            errorMessage = result.error.message
                        }
                        else -> {}
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            DappLabel(
                text = errorMessage ?: signResult,
                color = if (errorMessage != null) { Color.Red } else { Color.Unspecified },
                modifier = Modifier.padding(bottom = 36.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Preview
@Composable
fun PreviewBatchSignMessage() {
    BatchSignMessageScreen(
        rememberNavController(),
        ethereumState = EthereumState("", "", ""),
        batchSign = { _, _ -> Result.Success.Items(listOf()) }
    )
}
