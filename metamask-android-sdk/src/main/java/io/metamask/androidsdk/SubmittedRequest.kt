package io.metamask.androidsdk

data class

SubmittedRequest(
    val request: RpcRequest,
    val callback: (Any?) -> Unit
)
