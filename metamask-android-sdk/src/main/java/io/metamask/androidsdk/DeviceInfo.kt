package io.metamask.androidsdk

import android.os.Build

object DeviceInfo {
    val platformDescription = "${Build.MANUFACTURER} ${Build.MODEL} ${Build.VERSION.RELEASE}"
}