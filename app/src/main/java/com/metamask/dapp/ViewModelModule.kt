package com.metamask.dapp

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.metamask.androidsdk.ApplicationRepository
import io.metamask.androidsdk.Ethereum
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ViewModelModule {

    @Provides
    @Singleton
    fun provideEthereum(repository: ApplicationRepository): Ethereum {
        return Ethereum(repository)
    }

    @Provides
    @Singleton
    fun provideScreensViewModel(ethereum: Ethereum): ScreensViewModel {
        return ScreensViewModel(ethereum)
    }
}