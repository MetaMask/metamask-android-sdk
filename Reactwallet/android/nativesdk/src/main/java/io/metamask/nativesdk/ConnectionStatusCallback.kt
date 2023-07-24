package io.metamask.nativesdk

interface ConnectionStatusCallback {
    fun onConnect()
    fun onDisconnect()
}