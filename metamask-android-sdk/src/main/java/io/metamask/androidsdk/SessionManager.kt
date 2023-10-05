package io.metamask.androidsdk

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import java.util.*

internal class SessionManager(
    private val store: SecureStorage,
    private var sessionDuration: Long = 7 * 24 * 3600 // 7 days default
) {
    private val sessionConfigKey: String = "SESSION_CONFIG_KEY"
    private val sessionConfigFile: String = "SESSION_CONFIG_FILE"

    var sessionId: String = getSessionConfig().sessionId

    fun updateSessionDuration(duration: Long) {
        Logger.log("SessionManager:: Session duration extended by: ${duration/3600.0/24.0} days")
        sessionDuration = duration
        val sessionId = getSessionConfig().sessionId
        val expiryDate = System.currentTimeMillis() + sessionDuration * 1000
        val sessionConfig = SessionConfig(sessionId, expiryDate)
        saveSessionConfig(sessionConfig)
    }

    private fun getSessionConfig(reset: Boolean = false): SessionConfig {
        if (reset) {
            store.clearValue(sessionConfigKey, sessionConfigFile)
            return makeNewSessionConfig()
        }

        val sessionConfigJson = store.getValue(sessionConfigKey, sessionConfigFile)
            ?: return makeNewSessionConfig()

        val type: Type = object : TypeToken<SessionConfig>() {}.type

        return try {
            val sessionConfig: SessionConfig = Gson().fromJson(sessionConfigJson, type)

            if (sessionConfig.isValid()) {
                sessionConfig
            } else {
                makeNewSessionConfig()
            }
        } catch(e: Exception) {
            Logger.error("SessionManager: ${e.message}")
            makeNewSessionConfig()
        }
    }

    private fun saveSessionConfig(sessionConfig: SessionConfig) {
        val sessionConfigJson = Gson().toJson(sessionConfig)
        store.putValue(sessionConfigJson, sessionConfigKey, sessionConfigFile)
    }

    fun clearSession() {
        store.clearValue(sessionConfigKey, sessionConfigFile)
        makeNewSessionConfig()
        sessionId = getSessionConfig().sessionId
    }

    private fun makeNewSessionConfig(): SessionConfig {
        val sessionId = UUID.randomUUID().toString()
        val expiryDate = System.currentTimeMillis() + sessionDuration * 1000
        val sessionConfig = SessionConfig(sessionId, expiryDate)
        saveSessionConfig(sessionConfig)
        return sessionConfig
    }
}