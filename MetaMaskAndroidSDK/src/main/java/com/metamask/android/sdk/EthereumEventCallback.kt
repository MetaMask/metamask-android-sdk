package com.metamask.android.sdk

interface EthereumEventCallback {
    fun updateAccount(account: String)
    fun updateChainId(chainId: String)
}