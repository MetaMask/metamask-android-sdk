package com.metamask.dapp

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.metamask.androidsdk.*
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
        return ethereum.ethSignTypedDataV4(typedData = message, address)
    }

    suspend fun getBalance(address: String, block: String = "latest") : Result {
        return ethereum.getEthBalance(address, block)
    }

    suspend fun gasPrice() : Result {
        return ethereum.getEthGasPrice()
    }

    suspend fun web3ClientVersion() : Result {
        return ethereum.getWeb3ClientVersion()
    }

    suspend fun sendTransaction(
        amount: String,
        from: String,
        to: String,
    ) : Result {
        return ethereum.sendTransaction(from = from, to = to, value = amount)
    }

    suspend fun switchChain(chainId: String) : SwitchChainResult {
        return when (val result = ethereum.switchEthereumChain(chainId)) {
            is Result.Success -> {
                SwitchChainResult.Success("Successfully switched to ${Network.chainNameFor(chainId)} ($chainId)")
            }
            is Result.Error -> {
                if (result.error.code == ErrorType.UNRECOGNIZED_CHAIN_ID.code || result.error.code == ErrorType.SERVER_ERROR.code) {
                    val message = "${Network.chainNameFor(chainId)} ($chainId) has not been added to your MetaMask wallet. Add chain?"
                    SwitchChainResult.Error(result.error.code, message)
                } else {
                    SwitchChainResult.Error(result.error.code,"Add chain error: ${result.error.message}")
                }
            }
        }
    }

    suspend fun addEthereumChain(chainId: String) : SwitchChainResult {
        return when (val result = ethereum.addEthereumChain(
            chainId = chainId,
            chainName = Network.chainNameFor(chainId),
            rpcUrls = Network.rpcUrls(Network.fromChainId(chainId)),
            iconUrls = listOf(),
            blockExplorerUrls = null,
            nativeCurrency = NativeCurrency(name = Network.chainNameFor(chainId), symbol = Network.symbol(chainId), decimals = 18)
        )) {
            is Result.Error -> {
                SwitchChainResult.Error(result.error.code,"Add chain error: ${result.error.message}")
            }
            is Result.Success -> {
                if (chainId == ethereum.chainId) {
                    SwitchChainResult.Success("Successfully switched to ${Network.chainNameFor(chainId)} ($chainId)")
                } else {
                    SwitchChainResult.Success("Successfully added ${Network.chainNameFor(chainId)} ($chainId)")
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
    data class Error(val error: Int, val message: String): SwitchChainResult()
}