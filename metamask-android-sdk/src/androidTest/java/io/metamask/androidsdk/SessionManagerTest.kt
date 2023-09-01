package io.metamask.androidsdk

import org.junit.Test
import org.junit.Before
import org.junit.Assert.*
import org.junit.runner.RunWith

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry

@RunWith(AndroidJUnit4::class)
class SessionManagerTest {
    private lateinit var context: Context
    private lateinit var keyStorage: SecureStorage

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        keyStorage = KeyStorage(context)
    }

    @Test
    fun testNewSessionIdIsNotEmpyty() {
        val sessionManager = SessionManager.getInstance(keyStorage)
        val sessionId = sessionManager.sessionId
        assert(sessionId.isNotEmpty())
    }

    @Test
    fun testSetSessionDuration() {
        val sessionDuration: Long = 2400
        val sessionManager = SessionManager.getInstance(keyStorage, sessionDuration)
        val sessionConfig = sessionManager.getSessionConfig()
        val expiryDate = System.currentTimeMillis() + sessionDuration * 1000 // in milliseconds
        assertEquals(sessionConfig.expiryDate/100, expiryDate/100)
    }

    @Test
    fun testUpdateSessionDuration() {
        val sessionDuration: Long = 3600
        val sessionManager = SessionManager.getInstance(keyStorage, sessionDuration)
        val sessionConfig = sessionManager.getSessionConfig(true)
        val expiryDate = System.currentTimeMillis() + sessionDuration * 1000 // in milliseconds
        assertEquals(sessionConfig.expiryDate/100, expiryDate/100) // cater for microsecond differences due to computation time

        val newSessionDuration: Long = 3600 * 24
        sessionManager.sessionDuration = newSessionDuration
        val newDurationSessionConfig = sessionManager.getSessionConfig()
        val newExpiryDate = System.currentTimeMillis() + newSessionDuration * 1000 // in milliseconds
        assertEquals(newDurationSessionConfig.expiryDate/100, newExpiryDate/100)
    }

    @Test
    fun testSessionIdDoesNotChangeWhenUpdatingSessionDuration() {
        val sessionManager = SessionManager.getInstance(keyStorage)
        val sessionIdBefore = sessionManager.sessionId

        sessionManager.sessionDuration = 3600 * 24
        val sessionIdAfter = sessionManager.sessionId
        assertEquals(sessionIdBefore, sessionIdAfter)
    }

    @Test
    fun testSessionIdUpdatedOnReset() {
        val sessionManager = SessionManager.getInstance(keyStorage)
        val sessionIdBefore = sessionManager.getSessionConfig().sessionId
        val sessionIdAfter = sessionManager.getSessionConfig(true).sessionId
        assertNotEquals(sessionIdBefore, sessionIdAfter)
    }

    @Test
    fun testClearSessionIdGeneratesNewSession() {
        val sessionManager = SessionManager.getInstance(keyStorage)
        val sessionIdBefore = sessionManager.getSessionConfig().sessionId

        sessionManager.clearSession()
        val sessionIdAfter = sessionManager.getSessionConfig()
        assertNotEquals(sessionIdBefore, sessionIdAfter)
    }
}