package io.metamask.androidsdk

import android.os.Bundle

interface RemoteMessageServiceCallback {
    fun initiateKeyHandshake()
    fun handleRemoteMessage(bundle: Bundle)
}