package com.metamask.dapp

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
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
fun SignMessageScreen(
    navController: NavController,
    ethereumState: EthereumState,
    isConnectSign: Boolean = false,
    connectSignMessage: suspend (message: String) -> Result<String>,
    signMessage: suspend (message: String, address: String) -> Result<String>
) {
    fun signMessage(chainId: String): String {
        return if(isConnectSign) {
"He will win who knows when to fight and when not to fight. He will win who knows how to handle both superior and inferior forces."
        } else {
            "{\"domain\":{\"chainId\":\"$chainId\",\"name\":\"Ether Mail\",\"verifyingContract\":\"0xCcCCccccCCCCcCCCCCCcCcCccCcCCCcCcccccccC\",\"version\":\"1\"},\"message\":{\"contents\":\"Hello, Busa!\",\"from\":{\"name\":\"Kinno\",\"wallets\":[\"0xCD2a3d9F938E13CD947Ec05AbC7FE734Df8DD826\",\"0xDeaDbeefdEAdbeefdEadbEEFdeadbeEFdEaDbeeF\"]},\"to\":[{\"name\":\"Busa\",\"wallets\":[\"0xbBbBBBBbbBBBbbbBbbBbbbbBBbBbbbbBbBbbBBbB\",\"0xB0BdaBea57B0BDABeA57b0bdABEA57b0BDabEa57\",\"0xB0B0b0b0b0b0B000000000000000000000000000\"]}]},\"primaryType\":\"Mail\",\"types\":{\"EIP712Domain\":[{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"version\",\"type\":\"string\"},{\"name\":\"chainId\",\"type\":\"uint256\"},{\"name\":\"verifyingContract\",\"type\":\"address\"}],\"Group\":[{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"members\",\"type\":\"Person[]\"}],\"Mail\":[{\"name\":\"from\",\"type\":\"Person\"},{\"name\":\"to\",\"type\":\"Person[]\"},{\"name\":\"contents\",\"type\":\"string\"}],\"Person\":[{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"wallets\",\"type\":\"address[]\"}]}}"
        }
    }

    var message = signMessage(ethereumState.chainId)
    var signResult by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(ethereumState.chainId) {
        message = signMessage(ethereumState.chainId)
    }

    Surface {
        AppTopBar(navController)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Heading(if (isConnectSign) { "Connect & Sign Message" } else { "Sign Message"})

            Spacer(modifier = Modifier.weight(1f))

            BasicTextField(
                value = message,
                textStyle = TextStyle(color = if (isSystemInDarkTheme()) { Color.White} else { Color.Black}),
                onValueChange = {
                     message = it
                },
                modifier = Modifier.padding(bottom = 36.dp)
            )

            if (isConnectSign) {
                DappButton(buttonText = stringResource(R.string.connect_sign)) {
                    coroutineScope.launch {
                        val result = connectSignMessage(message)
                        errorMessage = when (result) {
                            is Result.Success -> {
                                null
                            }
                            is Result.Error -> {
                                result.error.message
                            }
                        }
                    }
                }
            } else {
                DappButton(buttonText = stringResource(R.string.sign)) {
                    coroutineScope.launch {
                        when (val result = signMessage(message, ethereumState.selectedAddress)) {
                            is Result.Success -> {
                                errorMessage = null
                                signResult = result.value
                            }
                            is Result.Error -> {
                                errorMessage = result.error.message
                            }
                            else -> {}
                        }
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
fun PreviewSignMessage() {
    SignMessageScreen(
        rememberNavController(),
        ethereumState = EthereumState("", "", ""),
        false,
        signMessage = { _, _ -> Result.Success("") },
        connectSignMessage = { _ -> Result.Success("") }
    )
}