package com.metamask.dapp

import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.metamask.dapp.DappScreen.*
import io.metamask.androidsdk.*

@Composable
fun Setup(ethereumViewModel: EthereumViewModel, screenViewModel: ScreenViewModel) {
    val navController = rememberNavController()
    val ethereumState by ethereumViewModel.ethereumState.observeAsState(EthereumState("", "", ""))
    var isConnectWith by remember { mutableStateOf(false) }
    var isConnectSign by remember { mutableStateOf(false) }

    NavHost(navController = navController, startDestination = CONNECT.name) {
        composable(CONNECT.name) {
            ConnectScreen(
                ethereumState = ethereumState,
                onConnect = { onError ->
                    ethereumViewModel.connect(
                        onSuccess = { screenViewModel.setScreen(ACTIONS) },
                        onError) },
                onConnectSign = { screenViewModel.setScreen(CONNECT_SIGN_MESSAGE) },
                onConnectWith = { screenViewModel.setScreen(CONNECT_WITH) },
                onDisconnect = {
                    ethereumViewModel.disconnect()
                },
                onClearSession = {
                    ethereumViewModel.clearSession()
                }
            )
        }
        composable(ACTIONS.name) {
            DappActionsScreen(
                navController,
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
                connectSignMessage = { message, onSuccess, onError ->
                    ethereumViewModel.connectSign(message, onSuccess, onError)
                },
                signMessage = { message, address, onSuccess, onError ->
                    ethereumViewModel.signMessage(message, address, onSuccess, onError)
                }
            )
        }
        composable(BATCH_SIGN.name) {
            BatchSignMessageScreen(
                navController,
                ethereumState = ethereumState,
                batchSign = { messages, address, onSuccess, onError, ->
                    ethereumViewModel.sendBatchSigningRequest(messages, address, onSuccess, onError)
                }
            )
        }
        composable(SEND_TRANSACTION.name) {
            SendTransactionScreen(
                navController,
                ethereumState = ethereumState,
                isConnectWith,
                sendTransaction = { amount, from, to, onSuccess, onError ->
                    ethereumViewModel.sendTransaction(amount, from, to, onSuccess, onError)
                },
                connectWithSendTransaction = { amount, from, to, onSuccess, onError ->
                    ethereumViewModel.connectWithSendTransaction(amount, from, to, onSuccess, onError)
                }
            )
        }
        composable(SWITCH_CHAIN.name) {
            SwitchChainScreen(
                navController,
                ethereumState = ethereumState,
                switchChain = { chainId, onSuccess, onError ->
                    ethereumViewModel.switchChain(chainId, onSuccess, onError)
                }
            )
        }

        composable(READ_ONLY_CALLS.name) {
            ReadOnlyCallsScreen(
                navController,
                ethereumState = ethereumState,
                getBalance = { address, onSuccess, onError ->
                    ethereumViewModel.getBalance(address, onSuccess, onError)
                },
                getGasPrice = { onSuccess, onError ->
                    ethereumViewModel.gasPrice(onSuccess, onError)
                },
                getWeb3ClientVersion = { onSuccess, onError ->
                    ethereumViewModel.web3ClientVersion(onSuccess, onError)
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