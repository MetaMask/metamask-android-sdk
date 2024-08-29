package io.metamask.androidsdk

class MockInfuraProvider(private val infuraAPIKey: String, private val logger: Logger = TestLogger) : InfuraProvider(infuraAPIKey, logger) {
    var mockResponse: String? = null  // This can hold the mock response for the request.

    override fun makeRequest(request: RpcRequest, chainId: String, dappMetadata: DappMetadata, callback: ((Result) -> Unit)?) {
        if (mockResponse != null) {
            callback?.invoke(Result.Success(mockResponse!!))
        } else {
            callback?.invoke(Result.Error(RequestError(-1, "No response set in MockInfuraProvider")))
        }
    }
}