package io.metamask.androidsdk

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class MockKeyStorage : SecureStorage {
    val keyStoreAlias = "testStore"
    private val keyMap: MutableMap<String, SecretKey> = mutableMapOf()
    private val sharedPreferencesMap: MutableMap<String, MutableMap<String, String>> = mutableMapOf()

    override fun loadSecretKey() {
        val keyStoreAlias = "keyStoreAlias"
        keyMap[keyStoreAlias] = generateMockSecretKey()
    }

    override fun clear(file: String) {
        sharedPreferencesMap.remove(file)
    }

    override fun clearValue(key: String, file: String) {
        sharedPreferencesMap[file]?.remove(key)
    }

    override fun putValue(value: String, key: String, file: String) {
        val fileMap = sharedPreferencesMap.getOrPut(file) { mutableMapOf() }
        fileMap[key] = value
        sharedPreferencesMap[file] = fileMap
    }

    override suspend fun getValue(key: String, file: String): String? {
        val fileMap = sharedPreferencesMap[file]
        return fileMap?.get(key)
    }

    private fun generateMockSecretKey(): SecretKey {
        // Mock implementation: Generate a SecretKey for testing
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                keyStoreAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                .build()
        )
        return keyGenerator.generateKey()
    }
}
