package io.metamask.androidsdk

class MockReadOnlyRPCProvider(private val infuraAPIKey: String, private val readOnlyRPCMap: Map<String, String>?, private val logger: Logger = TestLogger) : ReadOnlyRPCProvider(infuraAPIKey, readOnlyRPCMap, logger) {
    var mockResponse: String? = null  // This can hold the mock response for the request.

    override fun makeRequest(request: RpcRequest, chainId: String, dappMetadata: DappMetadata, callback: ((Result) -> Unit)?) {
        if (mockResponse != null) {
            callback?.invoke(Result.Success.Item(mockResponse!!))
        } else {
            callback?.invoke(Result.Error(RequestError(-1, "No response set in MockInfuraProvider")))
        }
    }
}