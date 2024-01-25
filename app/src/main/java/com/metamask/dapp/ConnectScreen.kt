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

@Composable
fun ConnectScreen(
    ethereumState: EthereumState,
    onConnect: (onError: (message: String) -> Unit) -> Unit,
    onConnectSign: () -> Unit,
    onConnectWith: () -> Unit,
    onDisconnect: () -> Unit,
    onClearSession: () -> Unit) {

    val bottomMargin = 24.dp
    val connected = ethereumState.selectedAddress.isNotEmpty()

    var errorMessage by remember { mutableStateOf<String?>(null) }

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
                    onDisconnect()
                }
            } else {
                DappButton(buttonText = stringResource(R.string.connect)) {
                    onConnect() { error ->
                        errorMessage = error
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Connect and sign button
                DappButton(buttonText = stringResource(R.string.connect_sign)) {
                    onConnectSign()
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Connect with button
                DappButton(buttonText = stringResource(R.string.connect_with)) {
                    onConnectWith()
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
                onClearSession()
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
        onConnect = {_ ->},
        onConnectSign = {},
        onConnectWith = {},
        onDisconnect = {},
        onClearSession = {}
    )
}