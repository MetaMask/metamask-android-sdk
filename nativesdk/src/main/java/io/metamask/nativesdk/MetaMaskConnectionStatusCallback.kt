package io.metamask.nativesdk

interface MetaMaskConnectionStatusCallback {
    fun onMetaMaskReady()
    fun onMetaMaskConnect()
    fun onMetaMaskDisconnect()
    fun onMetaMaskBroadcastRegistered()
    fun onMetaMaskBroadcastUnregistered()
}