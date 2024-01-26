package io.metamask.androidsdk

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class KeyStorageTests {
    private lateinit var keyStorage: SecureStorage
    private val testFile = "testFile"


    @Before
    fun setUp() {
        keyStorage = MockKeyStorage()
        keyStorage.clear(testFile)
    }

    @Test
    fun testClearValue() = runBlocking {
        val testValue = "testValue"
        val testKey = "testKey"

        putValue(testValue, testKey)
        clearValue(testKey)
        delay(1000)

        val result = getValue(testKey)
        Assert.assertEquals(null, result)
    }

    @Test
    fun testClearAll() = runBlocking {
        val testValue1 = "testValue1"
        val testKey1 = "testKey1"

        val testValue2 = "testValue2"
        val testKey2 = "testKey2"

        putValue(testValue1, testKey1)
        putValue(testValue2, testKey2)

        clearAll()
        delay(1000)

        val result1 = getValue(testKey1)
        val result2 = getValue(testKey2)
        Assert.assertEquals(null, result1)
        Assert.assertEquals(null, result2)
    }

    @Test
    fun testPutValue() = runBlocking {
        val testValue = "testValue"
        val testKey = "testKey"

        putValue(testValue, testKey)
        delay(1000)

        val result = getValue(testKey)
        Assert.assertEquals(testValue, result)
    }

    @Test
    fun testGetValueNotFound() = runBlocking {
        val testKey = "testRandomKey"

        val result = getValue(testKey)
        Assert.assertEquals(null, result)
    }

    // Helper methods

    private fun clearAll() {
        keyStorage.clear(testFile)
    }

    private fun clearValue(key: String) {
        keyStorage.clearValue(key, testFile)
    }

    private fun putValue(value: String, key: String) {
        keyStorage.putValue(value, key, testFile)
    }

    private suspend fun getValue(key: String): String? {
        return keyStorage.getValue(key, testFile)
    }
}