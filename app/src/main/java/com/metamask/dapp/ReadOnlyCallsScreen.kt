package com.metamask.dapp

import androidx.compose.foundation.layout.*
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
import io.metamask.androidsdk.EthereumState
import io.metamask.androidsdk.Result
import kotlinx.coroutines.launch

@Composable
fun ReadOnlyCallsScreen(
    navController: NavController,
    ethereumState: EthereumState,
    getBalance: suspend (address: String) -> Result<String>,
    getGasPrice: suspend ()  -> Result<String>,
    getWeb3ClientVersion: suspend ()  -> Result<String>
) {
    var selectedAddress by remember { mutableStateOf("") }
    var balance by remember { mutableStateOf("") }
    var gasPrice by remember { mutableStateOf("") }
    var web3ClientVersion by remember { mutableStateOf("") }
    var getBalanceErrorMessage by remember { mutableStateOf<String?>(null) }
    var getGasPriceErrorMessage by remember { mutableStateOf<String?>(null) }
    var getWeb3VersionErrorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

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
                coroutineScope.launch {
                    when(val result = getBalance(selectedAddress)) {
                        is Result.Success -> {
                            balance = result.value
                            getBalanceErrorMessage = null
                        }
                        is Result.Error -> {
                            getBalanceErrorMessage = result.error.message
                        }
                        else -> {}
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            DappLabel(
                text = getBalanceErrorMessage ?: balance,
                color = if (getBalanceErrorMessage != null) { Color.Red } else { Color.Unspecified },
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Get gas price
            DappButton(buttonText = stringResource(R.string.get_gas_price)) {
                coroutineScope.launch {
                    when(val result = getGasPrice()) {
                        is Result.Success -> {
                            gasPrice = result.value
                            getGasPriceErrorMessage = null
                        }
                        is Result.Error -> {
                            getGasPriceErrorMessage = result.error.message
                        }
                        else -> {}
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            DappLabel(
                text = getGasPriceErrorMessage ?: gasPrice,
                color = if (getGasPriceErrorMessage != null) { Color.Red } else { Color.Unspecified },
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Get Web3 client version
            DappButton(buttonText = stringResource(R.string.get_web3_client_version)) {
                coroutineScope.launch {
                    when(val result = getWeb3ClientVersion()) {
                        is Result.Success -> {
                            web3ClientVersion = result.value
                            getWeb3VersionErrorMessage = null
                        }
                        is Result.Error -> {
                            getWeb3VersionErrorMessage = result.error.message
                        }
                        else -> {}
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            DappLabel(
                text = getWeb3VersionErrorMessage ?: web3ClientVersion,
                color = if (getWeb3VersionErrorMessage != null) { Color.Red } else { Color.Unspecified },
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
        getBalance = {_ -> Result.Success("")},
        getGasPrice = { -> Result.Success("")},
        getWeb3ClientVersion = { -> Result.Success("")}
    )
}