package io.metamask.androidsdk

import android.util.Log

class Logger {
    companion object {
        fun log(msg: String): Int {
            return Log.d(TAG, msg)
        }

        fun error(e: String): Int {
            return Log.e(TAG, e)
        }
    }
}