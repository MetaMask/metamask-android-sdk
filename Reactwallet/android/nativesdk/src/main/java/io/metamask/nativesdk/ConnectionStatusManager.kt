package io.metamask.nativesdk

import android.util.Log
import java.lang.ref.WeakReference

class ConnectionStatusManager private constructor() {
    private val connectionStatusCallbackRefs: MutableList<WeakReference<ConnectionStatusCallback>> = mutableListOf()

    companion object {
        private var instance: ConnectionStatusManager? = null

        fun getInstance(): ConnectionStatusManager {
            if (instance == null) {
                instance = ConnectionStatusManager()
            }
            return instance as ConnectionStatusManager
        }
    }

    fun addCallback(callback: ConnectionStatusCallback) {
        Logger.log("ConnectionStatusManager: Added connection status callback $callback")
        connectionStatusCallbackRefs.add(WeakReference(callback))
    }

    fun onConnect() {
        Logger.log("ConnectionStatusManager: metamask connected")
        connectionStatusCallbackRefs.forEach { callback ->
            callback.get()?.onConnect()
        }
    }

    fun onDisconnect() {
        Logger.log("ConnectionStatusManager: metamask disconnected")
        connectionStatusCallbackRefs.forEach { callback ->
            callback.get()?.onDisconnect()
        }
    }
}