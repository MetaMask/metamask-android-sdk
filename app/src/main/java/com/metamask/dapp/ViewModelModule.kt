package com.metamask.dapp

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.metamask.androidsdk.ApplicationRepository
import io.metamask.androidsdk.EthereumViewModel
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ViewModelModule {

    @Provides
    @Singleton
    fun provideEthereumViewModel(repository: ApplicationRepository): EthereumViewModel {
        return EthereumViewModel(repository)
    }

    @Provides
    @Singleton
    fun provideScreensViewModel(viewModel: EthereumViewModel): ScreensViewModel {
        return ScreensViewModel(viewModel)
    }
}
