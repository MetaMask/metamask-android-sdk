package com.metamask.dapp

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.metamask.dapp.com.metamask.dapp.AppTopBar
import io.metamask.androidsdk.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwitchChainScreen(
    navController: NavController,
    ethereumState: EthereumState,
    switchChain: (
        chainId: String,
        onSuccess: (message: String) -> Unit,
        onError: (message: String, action: (() -> Unit)?) -> Unit
    ) -> Unit
) {
    var networks by remember { mutableStateOf(
        enumValues<Network>()
            .toList()
            .filter { it.chainId != ethereumState.chainId }
    ) }

    var expanded by remember { mutableStateOf(false) }
    var targetNetwork by remember { mutableStateOf(networks[0]) }

    var snackbarData by remember { mutableStateOf<SnackbarData?>(null) }
    var resultMessage by remember { mutableStateOf<String?>(null) }
    var currentChainId by remember { mutableStateOf("") }

    LaunchedEffect(ethereumState.chainId) {
        // Collect the ethereumState.chainId whenever it changes
        // and update the networks list accordingly
        networks = enumValues<Network>()
            .toList()
            .filter { it.chainId != ethereumState.chainId }
        currentChainId = ethereumState.chainId

        if( networks.firstOrNull() != null) {
            targetNetwork = networks[0]
        }
    }

    Surface {
        AppTopBar(navController)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Heading("Switch Chain")

            Spacer(modifier = Modifier.weight(1f))

            Text(
                "Current: ${Network.chainNameFor(currentChainId)} (${currentChainId})",
                modifier = Modifier
                    .fillMaxWidth(),
                textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier
                        .fillMaxWidth(),
                ) {
                    TextField(
                        // The `menuAnchor` modifier must be passed to the text field for correctness.
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        readOnly = true,
                        value = Network.chainNameFor(targetNetwork.chainId),
                        onValueChange = {},
                        label = { Text("Target Network") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = ExposedDropdownMenuDefaults.textFieldColors(),
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        networks.forEach { network ->
                            DropdownMenuItem(
                                text = { Text(Network.name(network)) },
                                onClick = {
                                    targetNetwork = network
                                    expanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            DappButton(
                buttonText = if(snackbarData?.action != null)
                { stringResource(R.string.add_chain) }
                else { stringResource(R.string.switch_chain) }
            ) {
                if(snackbarData?.action != null) {
                    snackbarData?.action?.invoke()
                } else {
                    switchChain(
                        targetNetwork.chainId,  { message ->
                            resultMessage = message
                            snackbarData = null
                        }, { error, action ->
                            snackbarData = SnackbarData(error, action)
                            resultMessage = null
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            DappLabel(
                text = resultMessage ?: snackbarData?.message ?: "",
                color = if (snackbarData?.action != null) { Color.Red } else { Color.Unspecified },
                modifier = Modifier.padding(bottom = 36.dp)
            )

            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}

@Preview
@Composable
fun PreviewSwitchChain() {
    SwitchChainScreen(
        rememberNavController(),
        ethereumState = EthereumState("", "", ""),
        switchChain = { _, _, _ -> }
    )
}