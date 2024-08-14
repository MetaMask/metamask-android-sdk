package io.metamask.androidsdk

import android.content.Context
import io.metamask.androidsdk.MockTracker

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

    override fun provideTracker(): Tracker {
        return MockTracker()
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
        val tracker = provideTracker()
        val serviceConnection = provideClientServiceConnection()
        val clientMessageServiceCallback = provideClientMessageServiceCallback()

        return CommunicationClient(
            context,
            callback,
            sessionManager,
            keyExchange,
            serviceConnection,
            clientMessageServiceCallback,
            tracker,
            logger)
    }
}