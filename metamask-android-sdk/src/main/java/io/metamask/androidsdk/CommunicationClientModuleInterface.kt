package io.metamask.androidsdk

interface CommunicationClientModuleInterface {
    fun provideKeyStorage(): SecureStorage
    fun provideSessionManager(keyStorage: SecureStorage): SessionManager
    fun provideKeyExchange(): KeyExchange
    fun provideLogger(): Logger
    fun provideCommunicationClient(callback: EthereumEventCallback?): CommunicationClient
}