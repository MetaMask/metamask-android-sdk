package com.metamask.android.sdk

import kotlinx.coroutines.CompletableDeferred
import kotlinx.serialization.Serializable

data class SubmittedRequest<T>(
    val request: EthereumRequest<T>,
    val deferred: CompletableDeferred<Any>
)
