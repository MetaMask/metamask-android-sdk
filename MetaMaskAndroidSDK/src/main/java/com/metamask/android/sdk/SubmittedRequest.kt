package com.metamask.android.sdk

import kotlinx.coroutines.CompletableDeferred

data class SubmittedRequest(
    val request: EthereumRequest,
    val deferred: CompletableDeferred<Any>
)
