package io.metamask.androidsdk

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import io.metamask.nativesdk.IMessegeService
import io.metamask.nativesdk.IMessegeServiceCallback

open class ClientServiceConnection(
    var onConnected: (() -> Unit)? = null,
    var onDisconnected: ((ComponentName?) -> Unit)? = null,
    var onBindingDied: ((ComponentName?) -> Unit)? = null,
    var onNullBinding: ((ComponentName?) -> Unit)? = null
) : ServiceConnection {
    private var messageService: IMessegeService? = null

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        messageService = IMessegeService.Stub.asInterface(service)
        onConnected?.invoke()
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        onDisconnected?.invoke(name)
    }

    override fun onBindingDied(name: ComponentName?) {
        onBindingDied?.invoke(name)
    }

    override fun onNullBinding(name: ComponentName?) {
        onNullBinding?.invoke(name)
    }

    open fun registerCallback(callback: IMessegeServiceCallback) {
        messageService?.registerCallback(callback)
    }

    open fun sendMessage(bundle: Bundle) {
        messageService?.sendMessage(bundle)
    }
}