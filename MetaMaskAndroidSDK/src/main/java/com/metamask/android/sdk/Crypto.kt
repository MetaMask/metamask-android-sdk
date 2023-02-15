package com.metamask.android.sdk

import java.io.IOException

public interface Crypto {
    fun generatePrivateKey(): String
    fun publicKey(privateKey: String): String

    @Throws(IOException::class)
    fun encrypt(message: String, publicKey: String): String

    @Throws(IOException::class)
    fun decrypt(message: String, privateKey: String): String
}