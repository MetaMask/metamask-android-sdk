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
}

internal class EthereumFlow
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

    override fun disconnect(clearSession: Boolean) {
        ethereum.disconnect(clearSession)
    }
}