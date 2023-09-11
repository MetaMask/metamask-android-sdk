package io.metamask.androidsdk

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder

internal class ClientServiceConnection: ServiceConnection {
    var isServiceConnected = false
        private set

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        // Called when the service is connected
        isServiceConnected = true
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        // Called when the service is disconnected
        isServiceConnected = false
    }
}