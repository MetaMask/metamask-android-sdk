package com.metamask.dapp

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.metamask.androidsdk.EthereumState
import io.metamask.androidsdk.Result
import kotlinx.coroutines.launch

@Composable
fun ConnectScreen(
    ethereumState: EthereumState,
    connect: suspend () -> Result,
    connectSign: () -> Unit,
    connectWith: () -> Unit,
    disconnect: () -> Unit,
    clearSession: () -> Unit) {

    val bottomMargin = 24.dp
    val connected = ethereumState.selectedAddress.isNotEmpty()

    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    Surface {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Heading("MetaMask SDK Dapp")

            Spacer(modifier = Modifier.weight(1f))

            // Connect button
            if (connected) {
                DappButton(buttonText = stringResource(R.string.disconnect)) {
                    disconnect()
                }
            } else {
                DappButton(buttonText = stringResource(R.string.connect)) {
                    coroutineScope.launch {
                        errorMessage = when(val result = connect()) {
                            is Result.Error -> {
                                result.error.message
                            }
                            else -> { null }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Connect and sign button
                DappButton(buttonText = stringResource(R.string.connect_sign)) {
                    connectSign()
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Connect with button
                DappButton(buttonText = stringResource(R.string.connect_with)) {
                    connectWith()
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            DappLabel(
                text = errorMessage ?: ethereumState.selectedAddress,
                color = if (errorMessage != null) { Color.Red } else { Color.Unspecified },
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Clear session button
            DappButton(buttonText = stringResource(R.string.clear_session)) {
                clearSession()
                errorMessage = null
            }

            Spacer(modifier = Modifier.height(4.dp))

            DappLabel(
                text = ethereumState.sessionId,
                color = Color.Unspecified,
                modifier = Modifier.padding(bottom = bottomMargin)
            )
        }
    }
}

@Preview
@Composable
fun PreviewConnectClearButtons() {
    ConnectScreen(
        ethereumState = EthereumState("", "", ""),
        connect = { -> Result.Success.Item("")},
        connectSign = { },
        connectWith = { },
        disconnect = { },
        clearSession = { }
    )
}