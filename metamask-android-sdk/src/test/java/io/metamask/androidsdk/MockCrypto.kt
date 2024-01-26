package io.metamask.androidsdk

class MockCrypto() : Encryption {
    private val rsaEncryption = RSAEncryption()
    override var onInitialized: () -> Unit = {}

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