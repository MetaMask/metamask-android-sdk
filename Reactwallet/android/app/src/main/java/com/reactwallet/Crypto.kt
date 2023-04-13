package com.reactwallet

public interface Crypto {
    fun generatePrivateKey(): String

    fun publicKey(privateKey: String): String

    fun encrypt(message: String, publicKey: String): String

    fun decrypt(message: String, privateKey: String): String
}

public class Ecies: Crypto {
    override fun generatePrivateKey(): String {
        return ""
    }

    override fun publicKey(privateKey: String): String {
        return ""
    }

    override fun encrypt(message: String, publicKey: String): String {
        return ""
    }

    override fun decrypt(message: String, privateKey: String): String {
        return ""
    }
}