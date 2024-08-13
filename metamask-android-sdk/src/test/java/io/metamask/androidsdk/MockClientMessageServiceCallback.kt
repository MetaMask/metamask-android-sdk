package io.metamask.androidsdk

import android.os.Bundle

class MockClientMessageServiceCallback: ClientMessageServiceCallback() {
    var messageReceived = false
    var receivedMessage: Bundle? = null

    override fun onMessageReceived(bundle: Bundle) {
        super.onMessageReceived(bundle)
        messageReceived = true
        receivedMessage = bundle
    }
}