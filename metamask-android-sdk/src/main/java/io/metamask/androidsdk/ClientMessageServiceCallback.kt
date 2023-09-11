package io.metamask.androidsdk

import android.os.Bundle
import io.metamask.nativesdk.IMessegeServiceCallback

internal class ClientMessageServiceCallback(
    private val handleKeyExchange: (String) -> Unit,
    private val handleMessage: (String) -> Unit
) : IMessegeServiceCallback.Stub() {

    override fun onMessageReceived(bundle: Bundle) {
        val keyExchange = bundle.getString(KEY_EXCHANGE)
        val message = bundle.getString(MESSAGE)

        if (keyExchange != null) {
            handleKeyExchange(keyExchange)
        } else if (message != null) {
            handleMessage(message)
        }
    }
}