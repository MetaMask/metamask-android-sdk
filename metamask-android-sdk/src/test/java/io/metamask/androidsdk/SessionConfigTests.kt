package io.metamask.androidsdk

import org.junit.Test

class SessionConfigTests {
    companion object {
        const val SESSION_ID = "TEST_SESSION_ID"
    }

    @Test
    fun testSessionConfigIsValid() {
        val sessionDuration: Long = 7 * 24 * 3600 // 7 days
        val expiryDate = System.currentTimeMillis() + sessionDuration * 1000
        val sessionConfig = SessionConfig(SESSION_ID, expiryDate)
        assert(sessionConfig.isValid())
    }

    @Test
    fun testSessionConfigIsNotValid() {
        val sessionDuration: Long = 60 // 1 minute
        val expiryDate = System.currentTimeMillis() - sessionDuration * 1000 // 1 minute ago
        val sessionConfig = SessionConfig(SESSION_ID, expiryDate)
        assert(!sessionConfig.isValid())
    }
}