package io.metamask.androidsdk

interface CommunicationClientModuleInterface {
    fun provideKeyStorage(): SecureStorage
    fun provideSessionManager(keyStorage: SecureStorage): SessionManager
    fun provideKeyExchange(): KeyExchange
    fun provideLogger(): Logger
    fun provideTracker(): Tracker
    fun provideClientServiceConnection(): ClientServiceConnection
    fun provideClientMessageServiceCallback(): ClientMessageServiceCallback
    fun provideCommunicationClient(callback: EthereumEventCallback?): CommunicationClient
}