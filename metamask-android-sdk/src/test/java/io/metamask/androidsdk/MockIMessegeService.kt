package io.metamask.androidsdk

import io.metamask.nativesdk.IMessegeService
import io.metamask.nativesdk.IMessegeServiceCallback
import android.os.Bundle

class MockIMessegeService : IMessegeService.Stub() {
    var message: Bundle? = null

    // This holds the callback provided by clients of this mock service.
    private var callback: IMessegeServiceCallback? = null

    // This method is used by clients to register their callback implementation.
    override fun registerCallback(cb: IMessegeServiceCallback?) {
        this.callback = cb
    }

    // This method simulates sending a message and triggering the callback response.
    override fun sendMessage(msg: Bundle?) {
        message = msg
        callback?.onMessageReceived(msg)
    }
}

class MockIMessegeServiceCallback : IMessegeServiceCallback.Stub() {
    var response: Bundle? = null

    // This method will be triggered when the service sends a message.
    override fun onMessageReceived(msg: Bundle?) {
        response = msg
    }
}