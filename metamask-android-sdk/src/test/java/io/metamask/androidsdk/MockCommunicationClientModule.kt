package io.metamask.androidsdk

import android.content.Context
import io.metamask.androidsdk.MockTracker

class MockCommunicationClientModule(
    private val context: Context,
    private val keyStorage: SecureStorage,
    private val sessionManager: SessionManager,
    private val keyExchange: KeyExchange,
    private val serviceConnection: ClientServiceConnection,
    private val clientMessageServiceCallback: ClientMessageServiceCallback,
    private val tracker: Tracker,
    private val logger: Logger): CommunicationClientModuleInterface {

    override fun provideKeyStorage(): SecureStorage = keyStorage
    override fun provideSessionManager(keyStorage: SecureStorage): SessionManager = sessionManager
    override fun provideKeyExchange(): KeyExchange = keyExchange
    override fun provideLogger(): Logger = logger
    override fun provideTracker(): Tracker = tracker

    override fun provideClientServiceConnection(): ClientServiceConnection = serviceConnection
    override fun provideClientMessageServiceCallback(): ClientMessageServiceCallback = clientMessageServiceCallback
    override fun provideCommunicationClient(callback: EthereumEventCallback?): CommunicationClient = CommunicationClient(
        context,
        callback,
        sessionManager,
        keyExchange,
        serviceConnection,
        clientMessageServiceCallback,
        tracker,
        logger)
}