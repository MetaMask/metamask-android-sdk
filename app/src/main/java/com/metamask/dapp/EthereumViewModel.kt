package com.metamask.dapp

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.metamask.androidsdk.*
import java.util.*
import javax.inject.Inject

@HiltViewModel
class EthereumViewModel @Inject constructor(
    private val ethereum: Ethereum
    ): ViewModel() {

    val ethereumState = MediatorLiveData<EthereumState>().apply {
        addSource(ethereum.ethereumState) { newEthereumState ->
            value = newEthereumState
        }
    }

    fun connect(onSuccess: () -> Unit, onError: (String) -> Unit) {
        ethereum.connect() { result ->
            when (result) {
                is Result.Error -> {
                    Logger.log("Ethereum connection error: ${result.error.message}")
                    onError(result.error.message)
                }
                is Result.Success -> {
                    Logger.log("Ethereum connection result: $result")
                    onSuccess()
                }
            }
        }
    }

    fun connectWith(request: EthereumRequest, onSuccess: (Any?) -> Unit, onError: (String) -> Unit) {
        ethereum.connectWith(request) { result ->
            when (result) {
                is Result.Error -> {
                    Logger.log("Connectwith error: ${result.error.message}")
                    onError(result.error.message)
                }
                is Result.Success.Item -> {
                    Logger.log("Connectwith result: $result")
                    onSuccess(result.value)
                }
                else -> {}
            }
        }
    }

    fun connectWithSendTransaction(amount: String,
                        from: String,
                        to: String,
                        onSuccess: (Any?) -> Unit,
                        onError: (message: String) -> Unit) {
        val params: MutableMap<String, Any> = mutableMapOf(
            "from" to from,
            "to" to to,
            "amount" to amount
        )

        val transactionRequest = EthereumRequest(
            UUID.randomUUID().toString(),
            EthereumMethod.ETH_SEND_TRANSACTION.value,
            listOf(params)
        )

        connectWith(transactionRequest, onSuccess, onError)
    }

    fun connectAndSign(message: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        ethereum.connectAndSign(message) { result ->
            when (result) {
                is Result.Error -> {
                    Logger.log("Connect & sign error: ${result.error.message}")
                    onError(result.error.message)
                }
                is Result.Success.Item -> {
                    Logger.log("Connect & sign  result: $result")
                    onSuccess(result.value)
                }
                else -> {}
            }
        }
    }

    fun disconnect() {
        ethereum.disconnect()
    }

    fun clearSession() {
        ethereum.clearSession()
    }

    fun sendBatchSigningRequest(messages: List<String>,
                         address: String,
                         onSuccess: (List<String>) -> Unit,
                         onError: (message: String) -> Unit) {
        val requestBatch: MutableList<EthereumRequest> = mutableListOf()

        for (message in messages) {
            val params: List<String> = listOf(address, message)
            val ethereumRequest = EthereumRequest(
                method = EthereumMethod.PERSONAL_SIGN.value,
                params = params
            )
            requestBatch.add(ethereumRequest)
        }

        ethereum.sendRequestBatch(requestBatch) { result ->
            when (result) {
                is Result.Error -> {
                    Logger.log("Ethereum batch sign error: ${result.error.message}")
                    onError(result.error.message)
                }
                is Result.Success.Items -> {
                    Logger.log("Ethereum batch sign result: $result")
                    onSuccess(result.value)
                }
                else -> {}
            }
        }
    }

    fun signMessage(
        message: String,
        address: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val params: List<String> = listOf(address, message)

        val signRequest = EthereumRequest(
            UUID.randomUUID().toString(),
            EthereumMethod.ETH_SIGN_TYPED_DATA_V4.value,
            params
        )

        ethereum.sendRequest(signRequest) { result ->
            when (result) {
                is Result.Error -> {
                    Logger.log("Ethereum sign error: ${result.error.message}")
                    onError(result.error.message)
                }
                is Result.Success.Item -> {
                    Logger.log("Ethereum sign result: $result")
                    onSuccess(result.value)
                }
                else -> {}
            }
        }
    }

    fun sendTransaction(
        amount: String,
        from: String,
        to: String,
        onSuccess: (String) -> Unit,
        onError: (message: String) -> Unit
    ) {
        val params: MutableMap<String, Any> = mutableMapOf(
            "from" to from,
            "to" to to,
            "amount" to amount
        )

        val transactionRequest = EthereumRequest(
            UUID.randomUUID().toString(),
            EthereumMethod.ETH_SEND_TRANSACTION.value,
            listOf(params)
        )

        ethereum.sendRequest(transactionRequest) { result ->
            when (result) {
                is Result.Error -> {
                    Logger.log("Ethereum transaction error: ${result.error.message}")
                    onError(result.error.message)
                }
                is Result.Success.Item -> {
                    Logger.log("Ethereum transaction result: $result")
                    onSuccess(result.value)
                }
                else -> {}
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
            method = EthereumMethod.SWITCH_ETHEREUM_CHAIN.value,
            params = listOf(switchChainParams)
        )

        ethereum.sendRequest(switchChainRequest) { result ->
            when (result) {
                is Result.Error -> {
                    if (result.error.code == ErrorType.UNRECOGNIZED_CHAIN_ID.code || result.error.code == ErrorType.SERVER_ERROR.code) {
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
                        onError("Switch chain error: ${result.error.message}", null)
                    }
                }
                is Result.Success -> {
                    onSuccess("Successfully switched to ${Network.chainNameFor(chainId)} ($chainId)")
                }
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
            method = EthereumMethod.ADD_ETHEREUM_CHAIN.value,
            params = listOf(addChainParams)
        )

        ethereum.sendRequest(addChainRequest) { result ->
            when (result) {
                is Result.Error -> {
                    onError("Add chain error: ${result.error.message}")
                }
                is Result.Success -> {
                    if (chainId == ethereum.chainId) {
                        onSuccess("Successfully switched to ${Network.chainNameFor(chainId)} ($chainId)")
                    } else {
                        onSuccess("Successfully added ${Network.chainNameFor(chainId)} ($chainId)")
                    }
                }
            }
        }
    }
}