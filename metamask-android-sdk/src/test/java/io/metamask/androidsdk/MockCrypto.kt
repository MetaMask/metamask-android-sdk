package io.metamask.androidsdk

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MockCrypto() : Encryption {
    private val rsaEncryption: RSAEncryption = RSAEncryption()
    override var onInitialized: () -> Unit = {}

    init {
        onInitialized()
    }

    override fun generatePrivateKey(): String {
        return rsaEncryption.generatePrivateKey()
    }

    override fun publicKey(privateKey: String): String {
        return rsaEncryption.publicKey(privateKey)
    }

    override fun encrypt(publicKey: String, message: String): String {
        return rsaEncryption.encrypt(publicKey, message)
    }

    override fun decrypt(privateKey: String, message: String): String {
        return rsaEncryption.decrypt(privateKey, message)
    }
}