package io.metamask.androidsdk

interface EthereumEventCallback {
    fun updateAccount(account: String)
    fun updateChainId(chainId: String)
}