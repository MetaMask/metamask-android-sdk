package io.metamask.androidsdk

import android.os.Bundle

class MockClientMessageServiceCallback: ClientMessageServiceCallback() {
    var messageReceived = false

    override fun onMessageReceived(bundle: Bundle) {
        super.onMessageReceived(bundle)
        messageReceived = true
    }
}