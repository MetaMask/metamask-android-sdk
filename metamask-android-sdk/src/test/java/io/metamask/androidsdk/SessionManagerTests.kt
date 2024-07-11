package io.metamask.androidsdk

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SessionManagerTests {
    private val sessionConfigFile: String = "SESSION_CONFIG_FILE"
    private val sessionConfigKey: String = "SESSION_CONFIG_KEY"

    private lateinit var keyStorage: SecureStorage
    private lateinit var sessionManager: SessionManager
    @Before
    fun setUp() {
        keyStorage = MockKeyStorage()
        keyStorage.clearValue(key = sessionConfigKey, file = sessionConfigFile)
        keyStorage.clear(sessionConfigFile)
        sessionManager = SessionManager(store = keyStorage, logger = TestLogger)
        sessionManager.clearSession{}
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testInitLoadsSessionConfig() = runTest {
        assertNotNull(sessionManager.sessionId)
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testDefaultSessionDuration() = runTest {
        val sessionConfig = sessionManager.getSessionConfig()
        val defaultDuration = 30 * 24 * 3600L // 30 days
        assertEquals(sessionConfig.expiryDate, System.currentTimeMillis() + defaultDuration * 1000)
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testUpdateSessionDuration() = runTest {
        val newDuration: Long = 14 * 24 * 3600
        sessionManager.updateSessionDuration(newDuration)
        advanceUntilIdle()
        val sessionConfig = sessionManager.getSessionConfig()
        assertEquals(sessionConfig.expiryDate/1000, System.currentTimeMillis()/1000L + newDuration)
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testSessionConfigIsValid() = runTest {
        val sessionConfig = sessionManager.getSessionConfig()
        assertTrue(sessionConfig.isValid())
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testSessionConfigReset() = runBlocking {
        val initialSessionConfig = sessionManager.getSessionConfig()
        delay(1000)
        val resetSessionConfig = sessionManager.getSessionConfig(reset = true)

        assertNotEquals(initialSessionConfig.sessionId, resetSessionConfig.sessionId)
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testSaveSessionConfig() = runTest {
        val sessionConfig = SessionConfig("test_session", System.currentTimeMillis() + 1000L)
        sessionManager.saveSessionConfig(sessionConfig)

        val savedConfig = sessionManager.getSessionConfig()
        assertEquals(sessionConfig.sessionId, savedConfig.sessionId)
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testClearSession() = runTest {
        sessionManager.clearSession { }
        advanceUntilIdle()

        val sessionConfig = sessionManager.getSessionConfig()
        assertNotEquals("", sessionManager.sessionId)
        assertNotEquals("", sessionConfig.sessionId)
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testMakeNewSessionConfig() = runTest {
        val newConfig = sessionManager.makeNewSessionConfig()

        assertTrue(newConfig.isValid())
        assertNotEquals("", newConfig.sessionId)
    }
}