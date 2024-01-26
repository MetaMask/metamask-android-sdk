package io.metamask.androidsdk

interface Encryption {
    var onInitialized: () -> Unit
    fun generatePrivateKey(): String
    fun publicKey(privateKey: String): String
    fun encrypt(publicKey: String, message: String): String
    fun decrypt(privateKey: String, message: String): String
}