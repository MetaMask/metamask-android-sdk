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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.metamask.dapp.com.metamask.dapp.AppTopBar
import io.metamask.androidsdk.*

@Composable
fun ReadOnlyCallsScreen(
    navController: NavController,
    ethereumState: EthereumState,
    getBalance: (
        address: String,
        onSuccess: (String) -> Unit,
        onError: (message: String) -> Unit
    ) -> Unit,
    getGasPrice: (
        onSuccess: (String) -> Unit,
        onError: (message: String) -> Unit
    ) -> Unit,
    getWeb3ClientVersion: (
        onSuccess: (String) -> Unit,
        onError: (message: String) -> Unit
    ) -> Unit
) {
    var selectedAddress by remember { mutableStateOf("") }
    var balance by remember { mutableStateOf("") }
    var gasPrice by remember { mutableStateOf("") }
    var web3ClientVersion by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(ethereumState.selectedAddress) {
        selectedAddress = ethereumState.selectedAddress
    }

    Surface {
        AppTopBar(navController)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Heading("Read-Only RPCs")

            Spacer(modifier = Modifier.weight(1f))

            // Get balance
            DappButton(buttonText = stringResource(R.string.get_balance)) {
                getBalance(
                    selectedAddress,
                    { result ->
                        balance = result
                        errorMessage = null
                    },
                    { error ->
                        errorMessage = error
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            DappLabel(
                text = errorMessage ?: balance,
                color = if (errorMessage != null) { Color.Red } else { Color.Unspecified },
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Get gas price
            DappButton(buttonText = stringResource(R.string.get_gas_price)) {
                getGasPrice(
                    { result ->
                        gasPrice = result
                        errorMessage = null
                    },
                    { error ->
                        errorMessage = error
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            DappLabel(
                text = errorMessage ?: gasPrice,
                color = if (errorMessage != null) { Color.Red } else { Color.Unspecified },
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Get Web3 client version
            DappButton(buttonText = stringResource(R.string.get_web3_client_version)) {
                getWeb3ClientVersion(
                    { result ->
                        web3ClientVersion = result
                        errorMessage = null
                    },
                    { error ->
                        errorMessage = error
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            DappLabel(
                text = errorMessage ?: web3ClientVersion,
                color = if (errorMessage != null) { Color.Red } else { Color.Unspecified },
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
    }
}

@Preview
@Composable
fun PreviewReadOnlyCallsScreen() {
    ReadOnlyCallsScreen(
        rememberNavController(),
        ethereumState = EthereumState("", "", ""),
        getBalance = {_, _, _ ->},
        getGasPrice = {_, _ ->},
        getWeb3ClientVersion = {_, _ ->}
    )
}