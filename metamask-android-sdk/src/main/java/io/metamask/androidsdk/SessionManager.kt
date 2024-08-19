package io.metamask.androidsdk

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import java.lang.reflect.Type

class SessionManager(
    private val store: SecureStorage,
    var sessionDuration: Long = 30 * 24 * 3600, // 30 days default
    private val logger: Logger = DefaultLogger
) {
    var sessionId: String = ""

    var onInitialized: () -> Unit = {}
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    companion object {
        const val SESSION_CONFIG_KEY = "SESSION_CONFIG_KEY"
        const val SESSION_CONFIG_FILE = "SESSION_CONFIG_FILE"
        const val SESSION_ACCOUNT_KEY = "SESSION_ACCOUNT_KEY"
        const val SESSION_CHAIN_ID_KEY = "SESSION_CHAIN_ID_KEY"
        const val DEFAULT_SESSION_DURATION: Long = 30 * 24 * 3600 // 30 days default
    }    

    init {
        coroutineScope.launch {
            val id = getSessionConfig().sessionId
            sessionId = id
            onInitialized()
        }
    }

    fun updateSessionDuration(duration: Long) {
        logger.log("SessionManager:: Session duration extended by: ${duration/3600.0/24.0} days")
        coroutineScope.launch {
            sessionDuration = duration
            val sessionId = getSessionConfig().sessionId
            val expiryDate = System.currentTimeMillis() + sessionDuration * 1000
            val sessionConfig = SessionConfig(sessionId, expiryDate)
            saveSessionConfig(sessionConfig)
        }
    }

    suspend fun getSessionConfig(reset: Boolean = false): SessionConfig {
        if (reset) {
            store.clearValue(SESSION_CONFIG_KEY, SESSION_CONFIG_FILE)
            return makeNewSessionConfig()
        }

        val sessionConfigJson = store.getValue(SESSION_CONFIG_KEY, SESSION_CONFIG_FILE)
            ?: return makeNewSessionConfig()

        val type: Type = object : TypeToken<SessionConfig>() {}.type

        return try {
            val sessionConfig: SessionConfig = Gson().fromJson(sessionConfigJson, type)

            if (sessionConfig.isValid()) {
                SessionConfig(sessionConfig.sessionId, System.currentTimeMillis() + sessionDuration * 1000)
            } else {
                makeNewSessionConfig()
            }
        } catch(e: Exception) {
            logger.error("SessionManager: ${e.message}")
            makeNewSessionConfig()
        }
    }

    fun saveSessionConfig(sessionConfig: SessionConfig) {
        val sessionConfigJson = Gson().toJson(sessionConfig)
        store.putValue(sessionConfigJson, SESSION_CONFIG_KEY, SESSION_CONFIG_FILE)
    }

    fun clearSession(onComplete: () -> Unit) {
        coroutineScope.launch {
            store.clearValue(SESSION_CONFIG_KEY, SESSION_CONFIG_FILE)
            makeNewSessionConfig()
            sessionId = getSessionConfig().sessionId
            onComplete()
        }
    }

    fun makeNewSessionConfig(): SessionConfig {
        store.clear(SESSION_CONFIG_FILE)
        val sessionId = TimeStampGenerator.timestamp()
        val expiryDate = System.currentTimeMillis() + sessionDuration * 1000
        val sessionConfig = SessionConfig(sessionId, expiryDate)
        saveSessionConfig(sessionConfig)
        return sessionConfig
    }
}