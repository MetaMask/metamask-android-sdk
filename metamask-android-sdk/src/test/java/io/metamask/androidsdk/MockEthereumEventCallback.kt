package io.metamask.androidsdk

class MockEthereumEventCallback : EthereumEventCallback {
    var account: String = ""
    var chainId: String = ""
    override fun updateAccount(account: String) {
        this.account = account
    }

    override fun updateChainId(newChainId: String) {
        this.chainId = newChainId
    }
}