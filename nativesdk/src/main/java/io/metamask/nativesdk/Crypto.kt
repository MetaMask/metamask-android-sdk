package io.metamask.nativesdk

import com.crypto.ecies.Ecies

class Crypto {
    private val ecies = Ecies()

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