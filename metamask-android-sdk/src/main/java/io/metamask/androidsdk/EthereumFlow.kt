package io.metamask.androidsdk

import androidx.lifecycle.asFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.suspendCancellableCoroutine

interface EthereumFlowWrapper {
    val chainId: String
    val selectedAddress: String
    val ethereumState: Flow<EthereumState>
    suspend fun connect() : Result
    fun disconnect(clearSession: Boolean)
    suspend fun connectSign(message: String) : Result
    suspend fun connectWith(request: EthereumRequest) : Result
    suspend fun sendRequest(request: EthereumRequest) : Result
    suspend fun sendRequestBatch(requests: List<EthereumRequest>) : Result

    suspend fun getEthBalance(address: String, block: String) : Result
    suspend fun getEthAccounts() : Result

    suspend fun getChainId() : Result
    suspend fun getEthGasPrice() : Result
    suspend fun getEthBlockNumber() : Result
    suspend fun getEthEstimateGas() : Result
    suspend fun getWeb3ClientVersion() : Result

    suspend fun sendRawTransaction(signedTransaction: String) : Result
    suspend fun getBlockTransactionCountByHash(blockHash: String) : Result
    suspend fun getBlockTransactionCountByNumber(blockNumber: String) : Result
    suspend fun getTransactionCount(address: String, tagOrblockNumber: String) : Result
    suspend fun sendTransaction(from: String, to: String, amount: String) : Result

    suspend fun switchEthereumChain(targetChainId: String) : Result
    suspend fun addEthereumChain(chainId: String,
                                 chainName: String,
                                 rpcUrls: List<String>,
                                 iconUrls: List<String>?,
                                 blockExplorerUrls: List<String>?,
                                 nativeCurrency: NativeCurrency) : Result

    suspend fun personalSign(message: String, address: String) : Result
    suspend fun ethSignTypedDataV4(typedData: Any, address: String) : Result
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
    override suspend fun connect() : Result = suspendCancellableCoroutine { continuation ->
        ethereum.connect { result ->
            continuation.resume(result) {
                continuation.invokeOnCancellation {}
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun connectWith(request: EthereumRequest) : Result = suspendCancellableCoroutine { continuation ->
        ethereum.connectWith(request) { result ->
            continuation.resume(result) {
                continuation.invokeOnCancellation {}
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun connectSign(message: String) : Result = suspendCancellableCoroutine { continuation ->
        ethereum.connectSign(message) { result ->
            continuation.resume(result) {
                continuation.invokeOnCancellation {}
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun sendRequestBatch(requests: List<EthereumRequest>) : Result = suspendCancellableCoroutine { continuation ->
        ethereum.sendRequestBatch(requests) { result ->
            continuation.resume(result) {
                continuation.invokeOnCancellation {}
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun sendRequest(request: EthereumRequest) : Result = suspendCancellableCoroutine { continuation ->
        ethereum.sendRequest(request) { result ->
            continuation.resume(result) {
                continuation.invokeOnCancellation {}
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun ethereumRequest(method: EthereumMethod, params: Any?): Result = suspendCancellableCoroutine { continuation ->
        val request = EthereumRequest(method = method.value, params = params)

        ethereum.sendRequest(request) { result ->
            continuation.resume(result) {
                continuation.invokeOnCancellation {}
            }
        }
    }

    override suspend fun getChainId() : Result =
        ethereumRequest(method = EthereumMethod.ETH_CHAIN_ID, params = null)

    override suspend fun getEthAccounts() : Result =
        ethereumRequest(method = EthereumMethod.ETH_ACCOUNTS, params = null)

    override suspend fun getEthGasPrice() : Result =
        ethereumRequest(method = EthereumMethod.ETH_GAS_PRICE, params = listOf<String>())

    override suspend fun getEthBalance(address: String, block: String) : Result =
        ethereumRequest(method = EthereumMethod.ETH_GET_BALANCE, params = listOf(address, block))

    override suspend fun getEthBlockNumber() : Result =
        ethereumRequest(method = EthereumMethod.ETH_BLOCK_NUMBER, params = null)

    override suspend fun getEthEstimateGas() : Result =
        ethereumRequest(method = EthereumMethod.ETH_ESTIMATE_GAS, params = null)

    override suspend fun getWeb3ClientVersion() : Result =
        ethereumRequest(method = EthereumMethod.WEB3_CLIENT_VERSION, params = listOf<String>())

    override suspend fun personalSign(message: String, address: String) : Result =
        ethereumRequest(method = EthereumMethod.PERSONAL_SIGN, params = listOf(address, message))

    override suspend fun ethSignTypedDataV4(typedData: Any, address: String) : Result =
        ethereumRequest(method = EthereumMethod.ETH_SIGN_TYPED_DATA_V4, params = listOf(address, typedData))

    override suspend fun sendTransaction(from: String, to: String, amount: String) : Result =
        ethereumRequest(method = EthereumMethod.ETH_SEND_TRANSACTION, params = listOf(mapOf(
            "from" to from,
            "to" to to,
            "amount" to amount
        )))

    override suspend fun sendRawTransaction(signedTransaction: String) : Result =
        ethereumRequest(method = EthereumMethod.ETH_SEND_RAW_TRANSACTION, params = listOf(signedTransaction))


    override suspend fun getBlockTransactionCountByNumber(blockNumber: String): Result =
        ethereumRequest(method = EthereumMethod.ETH_GET_BLOCK_TRANSACTION_COUNT_BY_NUMBER, params = listOf(blockNumber))

    override suspend fun getBlockTransactionCountByHash(blockHash: String): Result =
        ethereumRequest(method = EthereumMethod.ETH_GET_BLOCK_TRANSACTION_COUNT_BY_HASH, params = listOf(blockHash))


    override suspend fun getTransactionCount(address: String, tagOrblockNumber: String): Result =
        ethereumRequest(method = EthereumMethod.ETH_GET_TRANSACTION_COUNT, params = listOf(address, tagOrblockNumber))


    override suspend fun addEthereumChain(chainId: String,
                                          chainName: String,
                                          rpcUrls: List<String>,
                                          iconUrls: List<String>?,
                                          blockExplorerUrls: List<String>?,
                                          nativeCurrency: NativeCurrency) : Result =
        ethereumRequest(method = EthereumMethod.ADD_ETHEREUM_CHAIN, params = listOf(mapOf(
            "chainId" to chainId,
            "chainName" to chainName,
            "rpcUrls" to rpcUrls,
            "iconUrls" to iconUrls,
            "blockExplorerUrls" to blockExplorerUrls,
            "nativeCurrency" to nativeCurrency
        )))

    override suspend fun switchEthereumChain(targetChainId: String): Result =
        ethereumRequest(method = EthereumMethod.SWITCH_ETHEREUM_CHAIN, params = listOf(mapOf("chainId" to targetChainId)))

    override fun disconnect(clearSession: Boolean) {
        ethereum.disconnect(clearSession)
    }
}