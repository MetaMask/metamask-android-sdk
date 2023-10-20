package io.metamask.androidsdk.coroutines

import io.metamask.androidsdk.Ethereum
import io.metamask.androidsdk.EthereumState
import io.metamask.androidsdk.EthereumStateListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

val Ethereum.etheriumStateFlow: Flow<EthereumState>
    get() = callbackFlow {
        val listener = EthereumStateListener(::trySendBlocking)
        addStateListener(listener)
        awaitClose { removeStateListener(listener) }
    }