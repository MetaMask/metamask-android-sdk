package io.metamask.androidsdk

import android.content.ComponentName
import android.os.Bundle
import android.os.IBinder
import io.metamask.nativesdk.IMessegeService
import io.metamask.nativesdk.IMessegeServiceCallback

class MockClientServiceConnection: ClientServiceConnection() {
    var serviceConectionCalled = false
    var serviceDisconnectionCalled = false
    var registerCallbackCalled = false
    var sendMessageCalled = false
    var sentMessage: Bundle? = null

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        super.onServiceConnected(name, service)
        serviceConectionCalled = true
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        onDisconnected?.invoke(name)
        serviceDisconnectionCalled = true
    }

    override fun onBindingDied(name: ComponentName?) {
        onBindingDied?.invoke(name)
    }

    override fun onNullBinding(name: ComponentName?) {
        onNullBinding?.invoke(name)
    }

    override fun registerCallback(callback: IMessegeServiceCallback) {
        super.registerCallback(callback)
        registerCallbackCalled = true
    }

    override fun sendMessage(bundle: Bundle) {
        super.sendMessage(bundle)
        sendMessageCalled = true
        sentMessage = bundle
    }
}