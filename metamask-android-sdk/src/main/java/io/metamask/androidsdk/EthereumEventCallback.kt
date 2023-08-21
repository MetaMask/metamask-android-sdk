package io.metamask.androidsdk

interface EthereumEventCallback {
    fun updateAccount(account: String)
    fun updateChainId(newChainId: String)
}