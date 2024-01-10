import io.metamask.androidsdk.*

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.runner.RunWith
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CompletableFuture

@RunWith(AndroidJUnit4::class)
class CryptoTests {
    private lateinit var crypto: Crypto
    lateinit var privateKey: String
    lateinit var publicKey: String

    @Before
    fun setup() {
        val initializationCompleted = CompletableFuture<Unit>()

        runBlocking {
            val coroutineScope = CoroutineScope(Dispatchers.IO)
            val job = coroutineScope.launch {
                crypto = Crypto()
                crypto.onInitialized = {
                    privateKey = crypto.generatePrivateKey()
                    publicKey = crypto.publicKey(privateKey)
                    initializationCompleted.complete(Unit)
                }
            }
            job.join()
            initializationCompleted.join()
        }
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