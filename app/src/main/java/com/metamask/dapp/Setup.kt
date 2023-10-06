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
    
    NavHost(navController = navController, startDestination = CONNECT.name) {
        composable(CONNECT.name) {
            ConnectScreen(
                ethereumState = ethereumState,
                onConnect = { dapp, onError ->
                    ethereumViewModel.connect(
                        dapp,
                        onSuccess = { screenViewModel.setScreen(ACTIONS) },
                        onError) },
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
                onSendTransaction = { screenViewModel.setScreen(SEND_TRANSACTION) },
                onSwitchChain = { screenViewModel.setScreen(SWITCH_CHAIN) }
            )
        }
        composable(SIGN_MESSAGE.name) {
            SignMessageScreen(
                navController,
                ethereumState = ethereumState,
                signMessage = { message, address, onSuccess, onError ->
                    ethereumViewModel.signMessage(message, address, onSuccess, onError)
                })
        }
        composable(SEND_TRANSACTION.name) {
            SendTransactionScreen(
                navController,
                ethereumState = ethereumState,
                sendTransaction = { amount, from, to, onSuccess, onError ->
                    ethereumViewModel.sendTransaction(amount, from, to, onSuccess, onError)
                })
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
    }

    when(screenViewModel.currentScreen.value) {
        CONNECT -> {
            navController.navigate(CONNECT.name)
        }
        ACTIONS -> {
            navController.navigate(ACTIONS.name)
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
    }
}