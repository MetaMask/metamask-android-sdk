package com.metamask.android.sdk

data class SessionConfig(
    val sessionId: String,
    val expiryDate: Long
) {
    fun isValid(): Boolean {
        return System.currentTimeMillis() < expiryDate
    }
}