package com.metamask.android.sdk

public interface Crypto {
    fun generatePrivateKey(): String

    fun publicKey(privateKey: String): String

    fun encrypt(message: String, publicKey: String): String

    fun decrypt(message: String, privateKey: String): String
}