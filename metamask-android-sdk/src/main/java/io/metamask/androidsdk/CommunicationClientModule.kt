package io.metamask.androidsdk

import android.content.Context

open class CommunicationClientModule(private val context: Context): CommunicationClientModuleInterface {
    override fun provideKeyStorage(): SecureStorage {
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

    override fun provideClientServiceConnection(): ClientServiceConnection {
        return ClientServiceConnection()
    }

    override fun provideClientMessageServiceCallback(): ClientMessageServiceCallback {
        return ClientMessageServiceCallback()
    }

    override fun provideCommunicationClient(callback: EthereumEventCallback?): CommunicationClient {
        val keyStorage = provideKeyStorage()
        val sessionManager = provideSessionManager(keyStorage)
        val keyExchange = provideKeyExchange()
        val serviceConnection = provideClientServiceConnection()
        val messageServiceCallback = provideClientMessageServiceCallback()
        val logger = provideLogger()

        return CommunicationClient(
            context,
            callback,
            sessionManager,
            keyExchange,
            serviceConnection,
            messageServiceCallback,
            logger)
    }
}