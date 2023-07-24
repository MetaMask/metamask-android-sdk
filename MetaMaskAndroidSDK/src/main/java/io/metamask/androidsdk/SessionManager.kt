package io.metamask.androidsdk

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import java.util.*

class SessionManager(
    private val store: SecureStorage,
    private var sessionDuration: Long = 7 * 24 * 3600 // 7 days default
) {
    private val sessionConfigKey: String = "SESSION_CONFIG_KEY"
    private val sessionConfigFile: String = "SESSION_CONFIG_FILE"

    val sessionId: String = getSessionConfig().sessionId

    fun setSessionDuration(duration: Long) {
        Logger.log("SessionManager: setSessionDuration")
        sessionDuration = duration
        var sessionId = getSessionConfig().sessionId
        val expiryDate = System.currentTimeMillis() + sessionDuration * 1000
        val sessionConfig = SessionConfig(sessionId, expiryDate)
        saveSessionConfig(sessionConfig)
    }

    fun getSessionConfig(reset: Boolean = false): SessionConfig {
        Logger.log("SessionManager: getSessionConfig")
        if (reset) {
            store.clearValue(sessionConfigKey, sessionConfigFile)
            return makeNewSessionConfig()
        }

        val sessionConfigJson = store.getValue(sessionConfigKey, sessionConfigFile)
            ?: return makeNewSessionConfig()

        val type: Type = object : TypeToken<SessionConfig>() {}.type

        Logger.log("SessionManager: fetchedSession $sessionConfigJson")

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
        Logger.log("SessionManager: saveSessionConfig")
        val sessionConfigJson = Gson().toJson(sessionConfig)
        store.putValue(sessionConfigJson, sessionConfigKey, sessionConfigFile)
    }

    fun clearSessionConfig() {
        store.clearValue(sessionConfigKey, sessionConfigFile)
        makeNewSessionConfig()
    }

    private fun makeNewSessionConfig(): SessionConfig {
        Logger.log("SessionManager: makeNewSessionConfig")
        val sessionId = UUID.randomUUID().toString()
        val expiryDate = System.currentTimeMillis() + sessionDuration * 1000
        val sessionConfig = SessionConfig(sessionId, expiryDate)
        saveSessionConfig(sessionConfig)
        return sessionConfig
    }
}