package com.metamask.dapp

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.metamask.androidsdk.*
import javax.inject.Inject

@HiltViewModel
class EthereumViewModel @Inject constructor(
    private val ethereum: Ethereum,
    private val logger: Logger = DefaultLogger
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
                    logger.log("Ethereum connection error: ${result.error.message}")
                    onError(result.error.message)
                }
                is Result.Success -> {
                    logger.log("Ethereum connection result: ${result.value.first()}")
                    onSuccess()
                }
                else -> { }
            }
        }
    }

    fun connectWith(request: EthereumRequest, onSuccess: (Any?) -> Unit, onError: (String) -> Unit) {
        ethereum.connectWith(request) { result ->
            when (result) {
                is Result.Error -> {
                    logger.log("Connectwith error: ${result.error.message}")
                    onError(result.error.message)
                }
                is Result.Success -> {
                    logger.log("Connectwith result: $result")
                    onSuccess(result.value)
                }
                else -> {}
            }
        }
    }

    fun connectWithSendTransaction(value: String,
                        from: String,
                        to: String,
                        onSuccess: (Any?) -> Unit,
                        onError: (message: String) -> Unit) {
        val params: MutableMap<String, Any> = mutableMapOf(
            "from" to from,
            "to" to to,
            "value" to value
        )

        val transactionRequest = EthereumRequest(
            method = EthereumMethod.ETH_SEND_TRANSACTION.value,
            params = listOf(params)
        )

        connectWith(transactionRequest, onSuccess, onError)
    }

    fun connectSign(message: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        ethereum.connectSign(message) { result ->
            when (result) {
                is Result.Error -> {
                    logger.log("Connect & sign error: ${result.error.message}")
                    onError(result.error.message)
                }
                is Result.Success -> {
                    logger.log("Connect & sign  result: $result")
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
                    logger.log("Ethereum batch sign error: ${result.error.message}")
                    onError(result.error.message)
                }
                is Result.Success -> {
                    logger.log("Ethereum batch sign result: $result")
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
        ethereum.ethSignTypedDataV4(message, address) { result ->
            when (result) {
                is Result.Error -> {
                    logger.log("Ethereum sign error: ${result.error.message}")
                    onError(result.error.message)
                }
                is Result.Success -> {
                    logger.log("Ethereum sign result: $result")
                    onSuccess(result.value)
                }
                else -> {}
            }
        }
    }

    fun getBalance(
        address: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        ethereum.getEthBalance(address, "latest") { result ->
            when (result) {
                is Result.Error -> {
                    logger.log("Ethereum get balance error: ${result.error.message}")
                    onError(result.error.message)
                }
                is Result.Success -> {
                    logger.log("Ethereum get balance result: $result")
                    onSuccess(result.value)
                }
                else -> {}
            }
        }
    }

    fun gasPrice(
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        ethereum.gasPrice { result ->
            when (result) {
                is Result.Error -> {
                    logger.log("Ethereum gas price error: ${result.error.message}")
                    onError(result.error.message)
                }
                is Result.Success -> {
                    logger.log("Ethereum gas price result: $result")
                    onSuccess(result.value)
                }
                else -> {}
            }
        }
    }

    fun web3ClientVersion(
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        ethereum.getWeb3ClientVersion { result ->
            when (result) {
                is Result.Error -> {
                    logger.log("Ethereum web3 client version error: ${result.error.message}")
                    onError(result.error.message)
                }
                is Result.Success -> {
                    logger.log("Ethereum web3 client version result: $result")
                    onSuccess(result.value)
                }
                else -> {}
            }
        }
    }

    fun sendTransaction(
        value: String,
        from: String,
        to: String,
        onSuccess: (String) -> Unit,
        onError: (message: String) -> Unit
    ) {
        ethereum.sendTransaction(from, to, value) { result ->
            when (result) {
                is Result.Error -> {
                    logger.log("Ethereum transaction error: ${result.error.message}")
                    onError(result.error.message)
                }
                is Result.Success -> {
                    logger.log("Ethereum transaction result: $result")
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
        ethereum.switchEthereumChain(chainId) { result ->
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
        logger.log("Adding chainId: $chainId")

        ethereum.addEthereumChain(chainId, Network.rpcUrls(Network.fromChainId(chainId))) { result ->
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