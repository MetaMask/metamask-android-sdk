package io.metamask.androidsdk

data class SessionConfig(
    val sessionId: String,
    val expiryDate: Long
) {
    fun isValid(): Boolean {
        return System.currentTimeMillis() < expiryDate
    }
}