package com.metamask.dapp

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.metamask.androidsdk.*
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ScreensViewModel @Inject constructor(
    private val ethereumViewModel: EthereumViewModel
    ): ViewModel() {

    private val _currentScreen = mutableStateOf(DappScreen.CONNECT)
    val currentScreen: State<DappScreen> = _currentScreen

    fun navigateTo(screen: DappScreen) {
        _currentScreen.value = screen
        Logger.log("Navigating to $screen")
    }

    fun connect(dapp: Dapp, onError: (message: String) -> Unit) {
        ethereumViewModel.connect(dapp) { result ->
            if (result is RequestError) {
                Logger.log("Ethereum connection error: ${result.message}")
                onError(result.message)
            } else {
                Logger.log("Ethereum connection result: $result")
                navigateTo(DappScreen.ACTIONS)
            }
        }
    }

    fun disconnect() {
        ethereumViewModel.disconnect()
    }

    fun clearSession() {
        ethereumViewModel.clearSession()
    }

    fun signMessage(
        message: String,
        callback: (Any?) -> Unit,
        onError: (message: String) -> Unit
    ) {
        val params: List<String> = listOf(ethereumViewModel.selectedAddress, message)

        val signRequest = EthereumRequest(
            UUID.randomUUID().toString(),
            EthereumMethod.ETHSIGNTYPEDDATAV4.value,
            params
        )

        ethereumViewModel.sendRequest(signRequest) { result ->
            if (result is RequestError) {
                onError(result.message)
                Logger.log("Ethereum sign error: ${result.message}")
            } else {
                Logger.log("Ethereum sign result: $result")
                callback(result)
            }
        }
    }

    fun sendTransaction(
        amount: String,
        from: String,
        to: String,
        onSuccess: (Any?) -> Unit,
        onError: (message: String) -> Unit
    ) {
        val params: MutableMap<String, Any> = mutableMapOf(
            "from" to from,
            "to" to to,
            "amount" to amount
        )

        val transactionRequest = EthereumRequest(
            UUID.randomUUID().toString(),
            EthereumMethod.ETHSENDTRANSACTION.value,
            listOf(params)
        )

        ethereumViewModel.sendRequest(transactionRequest) { result ->
            if (result is RequestError) {
                Logger.log("Ethereum transaction error: ${result.message}")
                onError(result.message)
            } else {
                Logger.log("Ethereum transaction result: $result")
                onSuccess(result)
            }
        }
    }

    fun switchChain(
        chainId: String,
        onSuccess: (message: String) -> Unit,
        onError: (message: String, action: (() -> Unit)?) -> Unit
    ) {
        val switchChainParams: Map<String, String> = mapOf("chainId" to chainId)
        val switchChainRequest = EthereumRequest(
            method = EthereumMethod.SWITCHETHEREUMCHAIN.value,
            params = listOf(switchChainParams)
        )

        ethereumViewModel.sendRequest(switchChainRequest) { result ->
            if (result is RequestError) {
                if (result.code == ErrorType.UNRECOGNIZEDCHAINID.code || result.code == ErrorType.SERVERERROR.code) {
                    val message = "${Network.chainNameFor(chainId)} ($chainId) has not been added to your MetaMask wallet. Add chain?"

                    val action: () -> Unit = {
                        addEthereumChain(
                            chainId,
                            onSuccess = { result ->
                                onSuccess(result)
                            },
                            onError = { error ->
                                onError(error, null)
                            }
                        )
                    }
                    onError(message, action)
                } else {
                    onError("Switch chain error: ${result.message}", null)
                }
            } else {
                onSuccess("Successfully switched to ${Network.chainNameFor(chainId)} ($chainId)")
            }
        }
    }

    private fun addEthereumChain(
        chainId: String,
        onSuccess: (message: String) -> Unit,
        onError: (message: String) -> Unit
    ) {
        Logger.log("Adding chainId: $chainId")

        val addChainParams: Map<String, Any> = mapOf(
            "chainId" to chainId,
            "chainName" to Network.chainNameFor(chainId),
            "rpcUrls" to Network.rpcUrls(Network.fromChainId(chainId))
        )
        val addChainRequest = EthereumRequest(
            method = EthereumMethod.ADDETHEREUMCHAIN.value,
            params = listOf(addChainParams)
        )

        ethereumViewModel.sendRequest(addChainRequest) { result ->
            if (result is RequestError) {
                onError("Add chain error: ${result.message}")
            } else {
                if (chainId == ethereumViewModel.chainId) {
                    onSuccess("Successfully switched to ${Network.chainNameFor(chainId)} ($chainId)")
                } else {
                    onSuccess("Successfully added ${Network.chainNameFor(chainId)} ($chainId)")
                }
            }
        }
    }
}