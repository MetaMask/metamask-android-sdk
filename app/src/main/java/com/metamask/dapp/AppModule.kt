package com.metamask.dapp

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.metamask.androidsdk.DappMetadata
import io.metamask.androidsdk.Ethereum
import io.metamask.androidsdk.InfuraProvider

@Module
@InstallIn(SingletonComponent::class)

internal object AppModule {
    @Provides
    fun provideDappMetadata(): DappMetadata {
        return DappMetadata("Droiddapp", "https://droiddapp.io", iconUrl = "https://cdn.sstatic.net/Sites/stackoverflow/Img/apple-touch-icon.png")
    }

    @Provides
    fun provideEthereum(@ApplicationContext context: Context, dappMetadata: DappMetadata): Ethereum {
        return Ethereum(context, dappMetadata, infuraProvider = InfuraProvider(infuraAPIKey = "66730f93f60248a698535d89f0f85554"))
    }
}
