package io.metamask.androidsdk

import android.util.Log

interface Logger {
    fun log(message: String)
    fun error(message: String)
}

object DefaultLogger : Logger {
    override fun log(message: String) {
        Log.d(TAG, message)
    }

    override fun error(message: String) {
        Log.e(TAG, message)
    }
}