import io.metamask.androidsdk.*

import org.junit.Test
import org.junit.Before
import org.junit.Assert.*
import org.junit.runner.RunWith

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

@RunWith(AndroidJUnit4::class)
class KeyStorageTests {
    private lateinit var keyStorage: SecureStorage
    private val testFile = "testFile"

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        keyStorage = KeyStorage(context)
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
        assertEquals(null, result)
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
        assertEquals(null, result1)
        assertEquals(null, result2)
    }

    @Test
    fun testPutValue() = runBlocking {
        val testValue = "testValue"
        val testKey = "testKey"

        putValue(testValue, testKey)
        delay(1000)

        val result = getValue(testKey)
        assertEquals(testValue, result)
    }

    @Test
    fun testGetValueNotFound() = runBlocking {
        val testKey = "testRandomKey"

        val result = getValue(testKey)
        assertEquals(null, result)
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