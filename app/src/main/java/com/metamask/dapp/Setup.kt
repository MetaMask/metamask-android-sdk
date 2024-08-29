package com.metamask.dapp

import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.metamask.dapp.DappScreen.*
import io.metamask.androidsdk.EthereumState
import io.metamask.androidsdk.Result

@Composable
fun Setup(ethereumViewModel: EthereumFlowViewModel, screenViewModel: ScreenViewModel) {
    val navController = rememberNavController()

    val ethereumState by ethereumViewModel.ethereumFlow.collectAsState(initial = EthereumState("", "", ""))

    var isConnectWith by remember { mutableStateOf(false) }
    var isConnectSign by remember { mutableStateOf(false) }

    var isConnecting by remember { mutableStateOf(false) }
    var isConnectSigning by remember { mutableStateOf(false) }
    var connectResult by remember { mutableStateOf<Result<String>>(Result.Success("")) }
    var account by remember { mutableStateOf(ethereumState.selectedAddress) }

    LaunchedEffect(ethereumState.selectedAddress) {
        if (ethereumState.selectedAddress.isNotEmpty()) {
            screenViewModel.setScreen(ACTIONS)
        }
    }

    // Connect
    LaunchedEffect(isConnecting) {
        if (isConnecting) {
            when (val result = ethereumViewModel.connect()) {
                is Result.Success -> {
                    connectResult = result
                    screenViewModel.setScreen(ACTIONS)
                }
                is Result.Error -> {
                    connectResult = result
                }
            }
            isConnecting = false
        }
    }

    NavHost(navController = navController, startDestination = if (account.isNotEmpty()) { DappScreen.ACTIONS.name } else { DappScreen.CONNECT.name }) {
        composable(CONNECT.name) {
            ConnectScreen(
                ethereumState = ethereumState,
                connect = {
                    isConnecting = true
                    connectResult
                          },
                connectSign = { screenViewModel.setScreen(CONNECT_SIGN_MESSAGE) },
                connectWith = { screenViewModel.setScreen(CONNECT_WITH) },
                disconnect = {
                    screenViewModel.setScreen(CONNECT)
                    ethereumViewModel.disconnect()
                },
                clearSession = {
                    screenViewModel.setScreen(CONNECT)
                    ethereumViewModel.disconnect(clearSession = true)
                }
            )
        }
        composable(ACTIONS.name) {
            DappActionsScreen(
                navController,
                ethereumState = ethereumState,
                onSignMessage = { screenViewModel.setScreen(SIGN_MESSAGE) },
                onChainedSign = { screenViewModel.setScreen(BATCH_SIGN) },
                onSendTransaction = { screenViewModel.setScreen(SEND_TRANSACTION) },
                onSwitchChain = { screenViewModel.setScreen(SWITCH_CHAIN) },
                onReadOnlyCalls = { screenViewModel.setScreen(READ_ONLY_CALLS) }
            )
        }
        composable(SIGN_MESSAGE.name) {
            SignMessageScreen(
                navController,
                ethereumState = ethereumState,
                isConnectSign,
                connectSignMessage = { message ->
                    isConnectSigning = true
                    ethereumViewModel.connectSign(message)
                },
                signMessage = { message, address ->
                    ethereumViewModel.signMessage(message, address)
                }
            )
        }
        composable(BATCH_SIGN.name) {
            BatchSignMessageScreen(
                navController,
                ethereumState = ethereumState,
                batchSign = { messages, address ->
                    ethereumViewModel.sendBatchSigningRequest(messages, address)
                }
            )
        }
        composable(SEND_TRANSACTION.name) {
            SendTransactionScreen(
                navController,
                ethereumState = ethereumState,
                isConnectWith,
                sendTransaction = { amount, from, to ->
                    ethereumViewModel.sendTransaction(amount, from, to)
                },
                connectWithSendTransaction = { amount, from, to ->
                    ethereumViewModel.connectWithSendTransaction(amount, from, to)
                }
            )
        }
        composable(SWITCH_CHAIN.name) {
            SwitchChainScreen(
                navController,
                ethereumState = ethereumState,
                switchChain = { chainId ->
                    ethereumViewModel.switchChain(chainId)
                },
                addChain = { chainId ->
                    ethereumViewModel.addEthereumChain(chainId)
                }
            )
        }

        composable(READ_ONLY_CALLS.name) {
            ReadOnlyCallsScreen(
                navController,
                ethereumState = ethereumState,
                getBalance = { address ->
                    ethereumViewModel.getBalance(address)
                },
                getGasPrice = {
                    ethereumViewModel.gasPrice()
                },
                getWeb3ClientVersion = {
                    ethereumViewModel.web3ClientVersion()
                }
            )
        }
    }

    when(screenViewModel.currentScreen.value) {
        CONNECT -> {
            navController.navigate(CONNECT.name)
        }
        ACTIONS -> {
            navController.navigate(ACTIONS.name)
        }
        CONNECT_SIGN_MESSAGE -> {
            isConnectSign = true
            navController.navigate(SIGN_MESSAGE.name)
        }
        BATCH_SIGN -> {
            navController.navigate(BATCH_SIGN.name)
        }
        CONNECT_WITH -> {
            isConnectWith = true
            navController.navigate(SEND_TRANSACTION.name)
        }
        SIGN_MESSAGE -> {
            navController.navigate(SIGN_MESSAGE.name)
        }
        SEND_TRANSACTION -> {
            navController.navigate(SEND_TRANSACTION.name)
        }
        SWITCH_CHAIN -> {
            navController.navigate(SWITCH_CHAIN.name)
        }
        READ_ONLY_CALLS -> {
            navController.navigate(READ_ONLY_CALLS.name)
        }
    }
}
