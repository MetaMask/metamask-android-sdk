package io.metamask.androidsdk

object TimeStampGenerator {
    fun timestamp(): String {
        return System.currentTimeMillis().toString()
    }
}