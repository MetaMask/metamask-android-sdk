package com.metamask.android.sdk

import kotlinx.coroutines.CompletableDeferred
import kotlinx.serialization.Serializable

data class SubmittedRequest(
    val request: EthereumRequest,
    val callback: (Any?) -> Unit
)
