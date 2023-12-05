package io.metamask.androidsdk

data class BatchRequest(
    override var id: String = TimeStampGenerator.timestamp(),
    override val method: String,
    override val params: List<EthereumRequest>
) : RpcRequest()
