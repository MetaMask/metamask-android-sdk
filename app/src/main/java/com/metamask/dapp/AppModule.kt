package com.metamask.dapp

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.metamask.androidsdk.EthereumViewModel
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideScreensViewModel(viewModel: EthereumViewModel): ScreensViewModel {
        return ScreensViewModel(viewModel)
    }
}
