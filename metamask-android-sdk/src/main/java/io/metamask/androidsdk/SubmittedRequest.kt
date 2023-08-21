package io.metamask.androidsdk

data class

SubmittedRequest(
    val request: EthereumRequest,
    val callback: (Any?) -> Unit
)
