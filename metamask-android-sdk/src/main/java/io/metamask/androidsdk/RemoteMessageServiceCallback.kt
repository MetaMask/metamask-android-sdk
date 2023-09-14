package io.metamask.androidsdk

import android.os.Bundle

interface RemoteMessageServiceCallback {
    fun initiateKeyExchange()
    fun handleRemoteMessage(bundle: Bundle)
}