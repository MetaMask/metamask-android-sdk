package io.metamask.androidsdk

object TestLogger : Logger {
    override fun log(message: String) {
        // No-op
    }

    override fun error(message: String) {
        // No-op
    }
}