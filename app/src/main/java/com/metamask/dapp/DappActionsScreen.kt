package com.metamask.dapp

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.metamask.dapp.com.metamask.dapp.AppTopBar

@Composable
fun DappActionsScreen(
    navController: NavController,
    onSignMessage: () -> Unit,
    onChainedSign: () -> Unit,
    onSendTransaction: () -> Unit,
    onSwitchChain: () -> Unit,
    onReadOnlyCalls: () -> Unit
) {
    Surface {
        AppTopBar(navController)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Heading("Dapp Actions")

            Spacer(modifier = Modifier.weight(1f))

            // Sign message button
            DappButton(buttonText = stringResource(R.string.sign)) {
                onSignMessage()
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Chained signing button
            DappButton(buttonText = stringResource(R.string.batch_sign)) {
                onChainedSign()
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Send transaction button
            DappButton(buttonText = stringResource(R.string.send)) {
                onSendTransaction()
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Switch chain button
            DappButton(buttonText = stringResource(R.string.switch_chain)) {
                onSwitchChain()
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Read only RPC calls
            DappButton(buttonText = stringResource(R.string.read_only_calls)) {
                onReadOnlyCalls()
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}



@Preview
@Composable
fun PreviewDappActions() {
    DappActionsScreen(rememberNavController(), {}, {}, {}, {}, {})
}