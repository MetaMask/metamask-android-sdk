package io.metamask.androidsdk

import org.junit.Assert
import org.junit.Before
import org.junit.Test

class CryptoTest {
    private lateinit var crypto: Crypto
    lateinit var privateKey: String
    lateinit var publicKey: String

    @Before
    fun setup() {
        crypto = Crypto()
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