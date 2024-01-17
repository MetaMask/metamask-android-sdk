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
import io.metamask.androidsdk.SDKOptions

@Module
@InstallIn(SingletonComponent::class)

internal object AppModule {
    @Provides
    fun provideDappMetadata(): DappMetadata {
        return DappMetadata("Droiddapp", "https://droiddapp.io", iconUrl = "https://cdn.sstatic.net/Sites/stackoverflow/Img/apple-touch-icon.png")
    }

    @Provides // Add SDKOptions(infuraAPIKey="supply_your_key_here") to Ethereum constructor for read-only calls
    fun provideEthereum(@ApplicationContext context: Context, dappMetadata: DappMetadata): Ethereum {
        return Ethereum(context, dappMetadata)
    }
}
