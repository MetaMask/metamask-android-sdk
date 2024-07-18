package io.metamask.androidsdk

import android.content.Context

class CommunicationClientModule(private val context: Context): CommunicationClientModuleInterface {
    override fun provideKeyStorage(): KeyStorage {
        return KeyStorage(context)
    }

    override fun provideSessionManager(keyStorage: SecureStorage): SessionManager {
        return SessionManager(keyStorage)
    }

    override fun provideKeyExchange(): KeyExchange {
        return KeyExchange()
    }

    override fun provideLogger(): Logger {
        return DefaultLogger
    }

    override fun provideCommunicationClient(callback: EthereumEventCallback?): CommunicationClient {
        val keyStorage = provideKeyStorage()
        val sessionManager = provideSessionManager(keyStorage)
        val keyExchange = provideKeyExchange()
        val logger = provideLogger()
        return CommunicationClient(context, callback, sessionManager, keyExchange, logger)
    }
}