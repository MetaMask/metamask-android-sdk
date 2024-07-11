package io.metamask.androidsdk

import io.metamask.nativesdk.IMessegeService
import io.metamask.nativesdk.IMessegeServiceCallback
import android.os.Bundle

class MockIMessegeService : IMessegeService.Stub() {
    var message: Bundle? = null

    private var callback: IMessegeServiceCallback? = null

    override fun registerCallback(callback: IMessegeServiceCallback?) {
        this.callback = callback
    }

    override fun sendMessage(msg: Bundle?) {
        message = msg
        callback?.onMessageReceived(msg)
    }
}

class MockIMessegeServiceCallback : IMessegeServiceCallback.Stub() {
    var response: Bundle? = null
    override fun onMessageReceived(msg: Bundle?) {
        response = msg
    }
}