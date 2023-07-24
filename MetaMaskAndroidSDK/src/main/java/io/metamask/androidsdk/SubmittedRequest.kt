package io.metamask.androidsdk

import kotlinx.coroutines.CompletableDeferred
import kotlinx.serialization.Serializable

data class SubmittedRequest(
    val request: EthereumRequest,
    val callback: (Any?) -> Unit
)
