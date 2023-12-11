package io.metamask.androidsdk

data class AnyRequest(
    override var id: String = TimeStampGenerator.timestamp(),
    override val method: String,
    override val params: Any?
) : RpcRequest()

