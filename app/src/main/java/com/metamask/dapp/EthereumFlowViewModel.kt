package com.metamask.dapp

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.metamask.androidsdk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class EthereumFlowViewModel @Inject constructor(
    private val ethereum: EthereumFlowWrapper
): ViewModel() {
    
    val ethereumFlow: Flow<EthereumState> get() = ethereum.ethereumState

    suspend fun connect() : Result {
        return ethereum.connect()
    }

    suspend fun connectWith(request: EthereumRequest) : Result {
        return ethereum.connectWith(request)
    }

    suspend fun connectSign(message: String) : Result {
        return ethereum.connectSign(message)
    }

    suspend fun connectWithSendTransaction(amount: String,
                                           from: String,
                                           to: String) : Result {
        val params: MutableMap<String, Any> = mutableMapOf(
            "from" to from,
            "to" to to,
            "amount" to amount
        )

        val transactionRequest = EthereumRequest(
            method = EthereumMethod.ETH_SEND_TRANSACTION.value,
            params = listOf(params)
        )

        return connectWith(transactionRequest)
    }

    suspend fun sendRequestBatch(requests: List<EthereumRequest>) : Result {
        return ethereum.sendRequestBatch(requests)
    }

    suspend fun sendRequest(request: EthereumRequest) : Result {
        return ethereum.sendRequest(request)
    }

    suspend fun sendBatchSigningRequest(
        messages: List<String>,
        address: String) : Result {
        val requestBatch: MutableList<EthereumRequest> = mutableListOf()

        for (message in messages) {
            val params: List<String> = listOf(address, message)
            val ethereumRequest = EthereumRequest(
                method = EthereumMethod.PERSONAL_SIGN.value,
                params = params
            )
            requestBatch.add(ethereumRequest)
        }

        return ethereum.sendRequestBatch(requestBatch)
    }

    suspend fun signMessage(
        message: String,
        address: String,
    ) : Result {
        val params: List<String> = listOf(address, message)

        val signRequest = EthereumRequest(
            method = EthereumMethod.ETH_SIGN_TYPED_DATA_V4.value,
            params = params
        )
        return ethereum.sendRequest(signRequest)
    }

    suspend fun getBalance(address: String) : Result {
        val params: List<String> = listOf(address, "latest")

        val getBalanceRequest = EthereumRequest(
            method = EthereumMethod.ETH_GET_BALANCE.value,
            params = params
        )

        return ethereum.sendRequest(getBalanceRequest)
    }

    suspend fun gasPrice() : Result {
        val params: List<String> = listOf()

        val gasPriceRequest = EthereumRequest(
            method = EthereumMethod.ETH_GAS_PRICE.value,
            params = params
        )

        return ethereum.sendRequest(gasPriceRequest)
    }

    suspend fun web3ClientVersion() : Result {
        val params: List<String> = listOf()

        val web3ClientVersionRequest = EthereumRequest(
            method = EthereumMethod.WEB3_CLIENT_VERSION.value,
            params = params
        )

        return ethereum.sendRequest(web3ClientVersionRequest)
    }

    suspend fun sendTransaction(
        amount: String,
        from: String,
        to: String,
    ) : Result {
        val params: MutableMap<String, Any> = mutableMapOf(
            "from" to from,
            "to" to to,
            "amount" to amount
        )

        val transactionRequest = EthereumRequest(
            method = EthereumMethod.ETH_SEND_TRANSACTION.value,
            params = listOf(params)
        )

        return ethereum.sendRequest(transactionRequest)
    }

    suspend fun switchChain(chainId: String) : SwitchChainResult {
        val switchChainParams: Map<String, String> = mapOf("chainId" to chainId)
        val switchChainRequest = EthereumRequest(
            method = EthereumMethod.SWITCH_ETHEREUM_CHAIN.value,
            params = listOf(switchChainParams)
        )

        return when (val result = ethereum.sendRequest(switchChainRequest)) {
            is Result.Error -> {
                if (result.error.code == ErrorType.UNRECOGNIZED_CHAIN_ID.code || result.error.code == ErrorType.SERVER_ERROR.code) {
                    val message = "${Network.chainNameFor(chainId)} ($chainId) has not been added to your MetaMask wallet. Add chain?"
                    SwitchChainResult.Error(message) {
                        CoroutineScope(Dispatchers.Main).launch {
                            when (val addChainResult = addEthereumChain(chainId)) {
                                is Result.Error -> {
                                    SwitchChainResult.Error(addChainResult.error.message, null)
                                }
                                is Result.Success -> {
                                    if (chainId == ethereum.chainId) {
                                        SwitchChainResult.Success(
                                            "Successfully switched to ${Network.chainNameFor(chainId)} ($chainId)"
                                        )
                                    } else {
                                        SwitchChainResult.Success("Successfully added ${Network.chainNameFor(chainId)} ($chainId)")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    SwitchChainResult.Error("Add chain error: ${result.error.message}", null)
                }
            }
            is Result.Success -> {
                SwitchChainResult.Success("Successfully switched to ${Network.chainNameFor(chainId)} ($chainId)")
            }
        }
    }

    private suspend fun addEthereumChain(chainId: String) : Result {
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

        return when (val result = ethereum.sendRequest(addChainRequest)) {
            is Result.Error -> {
                Result.Error(RequestError(result.error.code, "Add chain error: ${result.error.message}"))
            }
            is Result.Success -> {
                if (chainId == ethereum.chainId) {
                    Result.Success.Item("Successfully switched to ${Network.chainNameFor(chainId)} ($chainId)")
                } else {
                    Result.Success.Item("Successfully added ${Network.chainNameFor(chainId)} ($chainId)")
                }
            }
        }
    }

    fun disconnect(clearSession: Boolean = false) {
        ethereum.disconnect(clearSession)
    }
}

sealed class SwitchChainResult {
    data class Success(val value: String) : SwitchChainResult()
    data class Error(val error: String, val action: (() -> Unit)?): SwitchChainResult()
}