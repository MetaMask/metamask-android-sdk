package io.metamask.androidsdk

import android.os.Bundle
import io.metamask.nativesdk.IMessegeServiceCallback

open class ClientMessageServiceCallback(
    var onMessage: ((Bundle) -> Unit)? = null
) : IMessegeServiceCallback.Stub() {
    override fun onMessageReceived(bundle: Bundle) {
        onMessage?.invoke(bundle)
    }
}