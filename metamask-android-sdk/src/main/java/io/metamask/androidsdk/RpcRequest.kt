package io.metamask.androidsdk

sealed class RpcRequest {
    abstract val id: String
    abstract val method: String
    abstract val params: Any?
}