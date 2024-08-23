package io.metamask.androidsdk

import androidx.lifecycle.asFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.suspendCancellableCoroutine

interface EthereumFlowWrapper {
    val chainId: String
    val selectedAddress: String
    val ethereumState: Flow<EthereumState>
    suspend fun connect() : Result<String>
    fun disconnect(clearSession: Boolean)
    suspend fun connectSign(message: String) : Result<String>
    suspend fun connectWith(request: EthereumRequest) : Result<String>
    suspend fun sendRequest(request: EthereumRequest) : Result<Any?>
    suspend fun sendRequestBatch(requests: List<EthereumRequest>) : Result<List<String>>

    suspend fun getEthBalance(address: String, block: String) : Result<String>
    suspend fun getEthAccounts() : Result<List<String>>

    suspend fun getChainId() : Result<String>
    suspend fun getEthGasPrice() : Result<String>
    suspend fun getEthEstimateGas() : Result<String>
    suspend fun getWeb3ClientVersion() : Result<String>

    suspend fun sendRawTransaction(signedTransaction: String) : Result<String>
    suspend fun getTransactionCount(address: String, tagOrblockNumber: String) : Result<String>
    suspend fun sendTransaction(from: String, to: String, value: String) : Result<String>

    suspend fun switchEthereumChain(targetChainId: String) : Result<Any?>
    suspend fun addEthereumChain(chainId: String,
                                 chainName: String,
                                 rpcUrls: List<String>,
                                 iconUrls: List<String>?,
                                 blockExplorerUrls: List<String>?,
                                 nativeCurrency: NativeCurrency) : Result<Any?>

    suspend fun personalSign(message: String, address: String) : Result<String>
    suspend fun ethSignTypedDataV4(typedData: Any, address: String) : Result<String>
}

class EthereumFlow
constructor(
    private val ethereum: Ethereum,
) : EthereumFlowWrapper {

    override val chainId: String
        get() = ethereum.chainId

    override val selectedAddress: String
        get() = ethereum.selectedAddress

    override val ethereumState = ethereum.ethereumState.asFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun connect() : Result<String> = suspendCancellableCoroutine { continuation ->
        ethereum.connect { result ->
            continuation.resume(result) {
                continuation.invokeOnCancellation {}
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun connectWith(request: EthereumRequest) : Result<String> = suspendCancellableCoroutine { continuation ->
        ethereum.connectWith(request) { result ->
            continuation.resume(result) {
                continuation.invokeOnCancellation {}
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun connectSign(message: String) : Result<String> = suspendCancellableCoroutine { continuation ->
        ethereum.connectSign(message) { result ->
            continuation.resume(result) {
                continuation.invokeOnCancellation {}
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun sendRequestBatch(requests: List<EthereumRequest>) : Result<List<String>> = suspendCancellableCoroutine { continuation ->
        ethereum.sendRequestBatch(requests) { result ->
            continuation.resume(result) {
                continuation.invokeOnCancellation {}
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun sendRequest(request: EthereumRequest) : Result<Any?> = suspendCancellableCoroutine { continuation ->
        ethereum.sendRequest(request) { result ->
            continuation.resume(result) {
                continuation.invokeOnCancellation {}
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun <T> ethereumRequest(method: EthereumMethod, params: Any?): Result<T> = suspendCancellableCoroutine { continuation ->
        val request = EthereumRequest(method = method.value, params = params)

        ethereum.sendRequest(request) { result ->
            val convertedResult = when (result) {
                is Result.Success<*> -> {
                    try {
                        @Suppress("UNCHECKED_CAST")
                        Result.Success(result.value as T)
                    } catch (e: ClassCastException) {
                        Result.Error(RequestError(-1, "Type conversion error: ${e.message}"))
                    }
                }
                is Result.Error -> result
            }
            continuation.resume(convertedResult) {
                continuation.invokeOnCancellation {}
            }
        }
    }

    override suspend fun getChainId() : Result<String> =
        ethereumRequest(method = EthereumMethod.ETH_CHAIN_ID, params = null)

    override suspend fun getEthAccounts() : Result<List<String>> =
        ethereumRequest(method = EthereumMethod.ETH_ACCOUNTS, params = null)

    override suspend fun getEthGasPrice() : Result<String> =
        ethereumRequest(method = EthereumMethod.ETH_GAS_PRICE, params = listOf<String>())

    override suspend fun getEthBalance(address: String, block: String) : Result<String> =
        ethereumRequest(method = EthereumMethod.ETH_GET_BALANCE, params = listOf(address, block))

    override suspend fun getEthEstimateGas() : Result<String> =
        ethereumRequest(method = EthereumMethod.ETH_ESTIMATE_GAS, params = null)

    override suspend fun getWeb3ClientVersion() : Result<String> =
        ethereumRequest(method = EthereumMethod.WEB3_CLIENT_VERSION, params = listOf<String>())

    override suspend fun personalSign(message: String, address: String) : Result<String> =
        ethereumRequest(method = EthereumMethod.PERSONAL_SIGN, params = listOf(address, message))

    override suspend fun ethSignTypedDataV4(typedData: Any, address: String) : Result<String> =
        ethereumRequest(method = EthereumMethod.ETH_SIGN_TYPED_DATA_V4, params = listOf(address, typedData))

    override suspend fun sendTransaction(from: String, to: String, value: String) : Result<String> =
        ethereumRequest(method = EthereumMethod.ETH_SEND_TRANSACTION, params = listOf(mapOf(
            "from" to from,
            "to" to to,
            "value" to value
        )))

    override suspend fun sendRawTransaction(signedTransaction: String) : Result<String> =
        ethereumRequest(method = EthereumMethod.ETH_SEND_RAW_TRANSACTION, params = listOf(signedTransaction))

    override suspend fun getTransactionCount(address: String, tagOrblockNumber: String): Result<String> =
        ethereumRequest(method = EthereumMethod.ETH_GET_TRANSACTION_COUNT, params = listOf(address, tagOrblockNumber))


    override suspend fun addEthereumChain(chainId: String,
                                          chainName: String,
                                          rpcUrls: List<String>,
                                          iconUrls: List<String>?,
                                          blockExplorerUrls: List<String>?,
                                          nativeCurrency: NativeCurrency) : Result<Any?> =
        ethereumRequest(method = EthereumMethod.ADD_ETHEREUM_CHAIN, params = listOf(mapOf(
            "chainId" to chainId,
            "chainName" to chainName,
            "rpcUrls" to rpcUrls,
            "iconUrls" to iconUrls,
            "blockExplorerUrls" to blockExplorerUrls,
            "nativeCurrency" to nativeCurrency
        )))

    override suspend fun switchEthereumChain(targetChainId: String): Result<Any?> =
        ethereumRequest(method = EthereumMethod.SWITCH_ETHEREUM_CHAIN, params = listOf(mapOf("chainId" to targetChainId)))

    override fun disconnect(clearSession: Boolean) {
        if (clearSession) {
            ethereum.clearSession()
        } else {
            ethereum.disconnect()
        }
    }
}