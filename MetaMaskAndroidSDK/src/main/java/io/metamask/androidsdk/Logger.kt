package io.metamask.androidsdk

import android.util.Log

class Logger {
    companion object {
        const val TAG = "MM_ANDROID_SDK"

        fun log(msg: String): Int {
            return Log.d(TAG, msg)
        }

        fun error(e: String): Int {
            return Log.e(TAG, e)
        }
    }
}