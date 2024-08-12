package io.metamask.androidsdk

import android.content.Context

class MockCommunicationClientModule(private val context: Context): CommunicationClientModuleInterface{
    override fun provideKeyStorage(): SecureStorage {
        return MockKeyStorage()
    }

    override fun provideSessionManager(keyStorage: SecureStorage): SessionManager {
        return SessionManager(keyStorage)
    }

    override fun provideKeyExchange(): KeyExchange {
        return KeyExchange(MockCrypto())
    }

    override fun provideLogger(): Logger {
        return TestLogger
    }

    override fun provideClientServiceConnection(): ClientServiceConnection {
        return MockClientServiceConnection()
    }

    override fun provideClientMessageServiceCallback(): ClientMessageServiceCallback {
        return MockClientMessageServiceCallback()
    }

    override fun provideCommunicationClient(callback: EthereumEventCallback?): CommunicationClient {
        val keyStorage = provideKeyStorage()
        val sessionManager = provideSessionManager(keyStorage)
        val keyExchange = provideKeyExchange()
        val logger = provideLogger()
        val serviceConnection = provideClientServiceConnection()
        val clientMessageServiceCallback = provideClientMessageServiceCallback()

        return CommunicationClient(
            context,
            callback,
            sessionManager,
            keyExchange,
            serviceConnection,
            clientMessageServiceCallback,
            logger)
    }
}