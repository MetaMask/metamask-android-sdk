package io.metamask.androidsdk

import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.KeyPair
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import javax.crypto.Cipher

class RSAEncryption : Encryption {

    override var onInitialized: () -> Unit = {}
    private val keyPair: KeyPair

    init {
        val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
        keyPairGenerator.initialize(2048)
        keyPair = keyPairGenerator.generateKeyPair()
    }

    override fun generatePrivateKey(): String {
        val privateKey = keyPair.private
        return Base64.getEncoder().encodeToString(privateKey.encoded)
    }

    override fun publicKey(privateKey: String): String {
        val publicKey = keyPair.public
        return Base64.getEncoder().encodeToString(publicKey.encoded)
    }

    override fun encrypt(publicKey: String, message: String): String {
        val keyFactory = KeyFactory.getInstance("RSA")
        val publicKeySpec = X509EncodedKeySpec(Base64.getDecoder().decode(publicKey))
        val publicK = keyFactory.generatePublic(publicKeySpec)

        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, publicK)

        val encryptedBytes = cipher.doFinal(message.toByteArray())
        return Base64.getEncoder().encodeToString(encryptedBytes)
    }

    override fun decrypt(privateKey: String, message: String): String {
        val keyFactory = KeyFactory.getInstance("RSA")
        val privateKeySpec = PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKey))
        val privateK = keyFactory.generatePrivate(privateKeySpec)

        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.DECRYPT_MODE, privateK)

        val decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(message))
        return String(decryptedBytes)
    }
}