package io.metamask.androidsdk

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Base64.decode
import android.util.Base64.encodeToString
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

internal class KeyStorage(private val context: Context): SecureStorage {

    private val keyStoreAlias = context.packageName
    private val androidKeyStore = "AndroidKeyStore"

    private lateinit var keyStore: KeyStore
    private lateinit var secretKey: SecretKey

    init {
        loadSecretKey()
    }

    private val secretKeyEntry: KeyStore.SecretKeyEntry? get() {
            return try {
                keyStore.getEntry(keyStoreAlias, null) as? KeyStore.SecretKeyEntry
            } catch(e: Exception) {
                Logger.error("KeyStorage: ${e.message}")
                null
            }
    }

    private fun encodedValue(value: String): String {
        val bytes = (value + keyStoreAlias).toByteArray()
        val base64 = encodeToString(bytes, Base64.DEFAULT)
        return base64.replace('/', '_').replace('=', '-').lowercase()
    }

    private fun loadSecretKey() {
        keyStore = KeyStore.getInstance(androidKeyStore)
        keyStore.load(null)
        secretKey = secretKeyEntry?.secretKey ?: generateSecretKey()
    }

    private fun generateSecretKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, androidKeyStore)
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            keyStoreAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
            .build()

        keyGenerator.init(keyGenParameterSpec)
        return keyGenerator.generateKey()
    }

    override fun clearValue(key: String, file: String) {
        val encodedKey = encodedValue(key)
        val encodedFileName = encodedValue(file)

        context.getSharedPreferences(
            encodedFileName,
            Context.MODE_PRIVATE)
            .edit()
            .putString(encodedKey, null)
            .apply()
    }

    override fun putValue(value: String, key: String, file: String) {
        val encodedKey = encodedValue(key)
        val encodedFileName = encodedValue(file)

        val bytes = value.toByteArray()
        val base64 = encodeToString(bytes, Base64.DEFAULT)

        context.getSharedPreferences(
            encodedFileName,
            Context.MODE_PRIVATE)
            .edit()
            .putString(encodedKey, base64)
            .apply()
    }

    override fun getValue(key: String, file: String): String? {
        val encodedKey = encodedValue(key)
        val encodedFileName = encodedValue(file)

        val base64 = context.getSharedPreferences(
            encodedFileName,
            Context.MODE_PRIVATE)
            .getString(encodedKey, null)

        if (base64 != null) {
            val bytes = decode(base64, Base64.DEFAULT)
            return bytes.toString(Charsets.UTF_8)
        }

        return null
    }
}