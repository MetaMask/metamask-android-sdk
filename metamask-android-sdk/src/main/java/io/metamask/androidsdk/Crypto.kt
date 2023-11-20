package io.metamask.androidsdk

import io.metamask.ecies.Ecies
import kotlinx.coroutines.*

internal class Crypto {
    private lateinit var ecies: Ecies
    var onInitialized: () -> Unit = {}
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    init {
        coroutineScope.launch {
            ecies = Ecies()
            onInitialized()
        }
    }

    fun generatePrivateKey(): String {
        return ecies.privateKey()
    }

    fun publicKey(privateKey: String): String {
        return ecies.publicKeyFrom(privateKey)
    }

    fun encrypt(publicKey: String, message: String): String {
        return ecies.encrypt(publicKey, message)
    }

    fun decrypt(privateKey: String, message: String): String {
        return ecies.decrypt(privateKey, message)
    }
}