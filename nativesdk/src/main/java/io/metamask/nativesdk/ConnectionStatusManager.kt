package io.metamask.nativesdk

import java.lang.ref.WeakReference

class ConnectionStatusManager private constructor() {
    private var connectionStatusCallbackRef: WeakReference<MetaMaskConnectionStatusCallback>? = null

    companion object {
        private var instance: ConnectionStatusManager? = null

        fun getInstance(): ConnectionStatusManager {
            if (instance == null) {
                instance = ConnectionStatusManager()
            }
            return instance as ConnectionStatusManager
        }
    }

    fun setCallback(callback: MetaMaskConnectionStatusCallback) {
        Logger.log("ConnectionStatusManager:: Added connection status callback $callback")
        connectionStatusCallbackRef = WeakReference(callback)
    }

    fun onMetaMaskConnect() {
        Logger.log("ConnectionStatusManager:: metamask connected")
        connectionStatusCallbackRef?.get()?.onMetaMaskConnect()
    }

    fun onMetaMaskReady() {
        Logger.log("ConnectionStatusManager:: metamask bound")
        connectionStatusCallbackRef?.get()?.onMetaMaskReady()
    }

    fun onMetaMaskDisconnect() {
        Logger.log("ConnectionStatusManager:: metamask disconnected")
        connectionStatusCallbackRef?.get()?.onMetaMaskDisconnect()
    }

    fun onMetaMaskBroadcastRegistered() {
        Logger.log("ConnectionStatusManager:: metamask broadcast registered")
        connectionStatusCallbackRef?.get()?.onMetaMaskBroadcastRegistered()
    }

    fun onMetaMaskBroadcastUnregistered() {
        Logger.log("ConnectionStatusManager:: metamask broadcast unregistered")
        connectionStatusCallbackRef?.get()?.onMetaMaskBroadcastUnregistered()
    }
}