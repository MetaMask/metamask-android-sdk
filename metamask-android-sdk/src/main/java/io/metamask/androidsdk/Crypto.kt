package io.metamask.androidsdk

import io.metamask.ecies.Ecies
import kotlinx.coroutines.*

class Crypto : Encryption {
    private lateinit var ecies: Ecies
    override var onInitialized: () -> Unit = {}
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    init {
        coroutineScope.launch {
            ecies = Ecies()
            onInitialized()
        }
    }

    override fun generatePrivateKey(): String {
        return ecies.privateKey()
    }

    override fun publicKey(privateKey: String): String {
        return ecies.publicKeyFrom(privateKey)
    }

    override fun encrypt(publicKey: String, message: String): String {
        return ecies.encrypt(publicKey, message)
    }

    override fun decrypt(privateKey: String, message: String): String {
        return ecies.decrypt(privateKey, message)
    }
}