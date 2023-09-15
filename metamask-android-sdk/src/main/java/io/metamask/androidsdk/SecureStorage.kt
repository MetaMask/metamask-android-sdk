package io.metamask.androidsdk

interface SecureStorage {
    fun clearAll(file: String)
    fun clearValue(key: String, file: String)
    fun putValue(value: String, key: String, file: String)
    fun getValue(key: String, file: String): String?
}