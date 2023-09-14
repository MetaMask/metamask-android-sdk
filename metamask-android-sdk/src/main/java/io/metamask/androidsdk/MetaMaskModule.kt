package io.metamask.androidsdk

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MetaMaskModule {
    @Provides
    @Singleton
    fun provideTracker(): Tracker {
        return Analytics()
    }

    @Provides
    @Singleton
    fun provideKeyExchange(): KeyExchange {
        return KeyExchange()
    }

    @Provides
    @Singleton
    fun provideSecureStorage(repository: ApplicationRepository): SecureStorage {
        return KeyStorage(repository.context)
    }

    @Provides
    @Singleton
    fun provideSessionManager(storage: SecureStorage): SessionManager {
        return SessionManager.getInstance(storage)
    }

    @Provides
    @Singleton
    fun provideRemoteServiceConnection(
        applicationRepository: ApplicationRepository,
        callback: Provider<RemoteMessageServiceCallback>
    ): RemoteServiceConnection {
        return RemoteServiceConnection(applicationRepository, callback)
    }

    @Provides
    @Singleton
    fun provideCommunicationClient(
        tracker: Tracker,
        keyExchange: KeyExchange,
        sessionManager: SessionManager,
        remoteServiceConnection: RemoteServiceConnection,
        callback: Provider<EthereumEventCallback>
    ): CommunicationClient {
        return CommunicationClient(tracker, keyExchange, callback, sessionManager, remoteServiceConnection)
    }

    @Provides
    @Singleton
    fun provideEthereumViewModel(
        applicationRepository: ApplicationRepository,
        communicationClient: CommunicationClient
    ): EthereumViewModel {
        return EthereumViewModel(applicationRepository, communicationClient)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AbstractMetaMaskModule {
    @Binds
    abstract fun bindEthereumEventCallback(viewModel: EthereumViewModel): EthereumEventCallback

    @Binds
    abstract fun bindRemoteMessageServiceCallback(client: CommunicationClient): RemoteMessageServiceCallback
}