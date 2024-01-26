package io.metamask.androidsdk

import org.junit.Assert
import org.junit.Before
import org.junit.Test

class CryptoTests {
    private lateinit var crypto: Encryption
    private lateinit var privateKey: String
    private lateinit var publicKey: String

    @Before
    fun setup() {
        crypto = MockCrypto()
        privateKey = crypto.generatePrivateKey()
        publicKey = crypto.publicKey(privateKey)
    }

    @Test
    fun testPrivateKeyIsNotNullOrEmpty() {
        assert(!privateKey.isNullOrEmpty())
    }

    @Test
    fun testPublicKeyIsNotNullOrEmpty() {
        assert(!publicKey.isNullOrEmpty())
    }

    @Test
    fun testEncryptDecrypt() {
        val plainText = "Text 2 encrypt!"
        val encryptedText = crypto.encrypt(publicKey, plainText)
        val decrypted = crypto.decrypt(privateKey, encryptedText)

        Assert.assertEquals(decrypted, plainText)
    }
}