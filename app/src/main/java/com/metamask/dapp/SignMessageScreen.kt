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
import io.metamask.androidsdk.*

@Composable
fun SignMessageScreen(
    navController: NavController,
    ethereumState: EthereumState,
    signMessage: (
        message: String,
        address: String,
        onSuccess: (Any?) -> Unit,
        onError: (message: String) -> Unit
    ) -> Unit
) {
    fun signMessage(chainId: String): String {
        return "{\"domain\":{\"chainId\":\"$chainId\",\"name\":\"Ether Mail\",\"verifyingContract\":\"0xCcCCccccCCCCcCCCCCCcCcCccCcCCCcCcccccccC\",\"version\":\"1\"},\"message\":{\"contents\":\"Hello, Busa!\",\"from\":{\"name\":\"Kinno\",\"wallets\":[\"0xCD2a3d9F938E13CD947Ec05AbC7FE734Df8DD826\",\"0xDeaDbeefdEAdbeefdEadbEEFdeadbeEFdEaDbeeF\"]},\"to\":[{\"name\":\"Busa\",\"wallets\":[\"0xbBbBBBBbbBBBbbbBbbBbbbbBBbBbbbbBbBbbBBbB\",\"0xB0BdaBea57B0BDABeA57b0bdABEA57b0BDabEa57\",\"0xB0B0b0b0b0b0B000000000000000000000000000\"]}]},\"primaryType\":\"Mail\",\"types\":{\"EIP712Domain\":[{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"version\",\"type\":\"string\"},{\"name\":\"chainId\",\"type\":\"uint256\"},{\"name\":\"verifyingContract\",\"type\":\"address\"}],\"Group\":[{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"members\",\"type\":\"Person[]\"}],\"Mail\":[{\"name\":\"from\",\"type\":\"Person\"},{\"name\":\"to\",\"type\":\"Person[]\"},{\"name\":\"contents\",\"type\":\"string\"}],\"Person\":[{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"wallets\",\"type\":\"address[]\"}]}}"
    }

    var message = signMessage(ethereumState.chainId)
    var signResult by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf<String?>(null) }

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
            Heading("Sign Message")

            Spacer(modifier = Modifier.weight(1f))

            BasicTextField(
                value = message,
                textStyle = TextStyle(color = if (isSystemInDarkTheme()) { Color.White} else { Color.Black}),
                 onValueChange = {
                     message = it
                },
                modifier = Modifier.padding(bottom = 36.dp)
            )

            DappButton(buttonText = stringResource(R.string.sign)) {
                signMessage(
                    message,
                    ethereumState.selectedAddress,
                    { result ->
                        signResult = result as String
                        errorMessage = null
                    },
                    { error ->
                        errorMessage = error
                    }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

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
        signMessage = { _, _, _, _ -> }
    )
}