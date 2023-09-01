package io.metamask.androidsdk

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import java.util.*

internal class SessionManager private constructor(
    private var store: SecureStorage,
    sessionLength: Long
) {
    companion object {
        private var instance: SessionManager? = null

        fun getInstance(storage: SecureStorage, sessionLength: Long = 7 * 24 * 3600): SessionManager {
            if (instance == null) {
                instance = SessionManager(storage, sessionLength)
            }
            instance?.store = storage
            instance?.sessionDuration = sessionLength
            return instance as SessionManager
        }
    }

    var sessionDuration: Long = sessionLength
        set(value) {
            field = value
            var sessionConfig = getSessionConfig()
            val expiryDate = expiryDate(value)
            val newSessionConfig = SessionConfig(sessionConfig.sessionId, expiryDate)
            saveSessionConfig(newSessionConfig)
        }

    private val sessionConfigKey: String = "SESSION_CONFIG_KEY"
    private val sessionConfigFile: String = "SESSION_CONFIG_FILE"

    var sessionId: String = getSessionConfig().sessionId

    fun getSessionConfig(reset: Boolean = false): SessionConfig {
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
        val newSessionConfig = makeNewSessionConfig()
        sessionId = newSessionConfig.sessionId
    }

    private fun makeNewSessionConfig(): SessionConfig {
        val sessionID = UUID.randomUUID().toString()
        val expDate = expiryDate(sessionDuration)
        val sessionConfig = SessionConfig(sessionID, expDate)
        saveSessionConfig(sessionConfig)
        return sessionConfig
    }

    private fun expiryDate(sessionDuration: Long): Long {
        return System.currentTimeMillis() + sessionDuration * 1000
    }
}