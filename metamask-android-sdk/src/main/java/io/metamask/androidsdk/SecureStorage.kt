package io.metamask.androidsdk

interface SecureStorage {
    fun loadSecretKey()
    fun clear(file: String)
    fun clearValue(key: String, file: String)
    fun putValue(value: String, key: String, file: String)
    suspend fun getValue(key: String, file: String): String?
}