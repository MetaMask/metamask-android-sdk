package com.metamask.dapp

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.metamask.androidsdk.*

@Module
@InstallIn(SingletonComponent::class)

internal object AppModule {
    @Provides
    fun provideDappMetadata(): DappMetadata {
        return DappMetadata("Droiddapp", "https://droiddapp.io", iconUrl = "https://cdn.sstatic.net/Sites/stackoverflow/Img/apple-touch-icon.png")
    }

    @Provides // Add SDKOptions(infuraAPIKey="supply_your_key_here") to Ethereum constructor for read-only calls
    fun provideEthereum(@ApplicationContext context: Context, dappMetadata: DappMetadata): Ethereum {
        return Ethereum(context, dappMetadata, SDKOptions(infuraAPIKey = "#####"))
    }

    @Provides
    fun provideEthereumFlow(ethereum: Ethereum): EthereumFlow {
        return EthereumFlow(ethereum)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class EthereumModule {
    @Binds
    abstract fun bindEthereum(ethereumflow: EthereumFlow): EthereumFlowWrapper
}
