package io.metamask.androidsdk

import android.content.Context
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

class CommunicationClientTest {

    @Mock
    private lateinit var context: Context

    private lateinit var callback: MockEthereumEventCallback
    private lateinit var logger: Logger
    private lateinit var crypto: Encryption
    private lateinit var keyExchange: KeyExchange
    private lateinit var sessionManager: SessionManager
    private lateinit var communicationClientModule: CommunicationClientModuleInterface
    private lateinit var communicationClient: CommunicationClient
    private lateinit var mockMessageService: MockIMessegeService

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        mockMessageService = (context as TestApplication).getMockService()
        callback = MockEthereumEventCallback()
        logger = TestLogger
        crypto = MockCrypto()
        keyExchange = KeyExchange(crypto = crypto, logger = logger)
        sessionManager = SessionManager(MockKeyStorage())
        communicationClientModule = CommunicationClientModule(context)

        communicationClient = CommunicationClient(context, callback, sessionManager, keyExchange, logger)

        // simulate service connection
        communicationClient.serviceConnection.onServiceConnected(null, mockMessageService.asBinder())
    }

    @Test
    fun testInit() {
        assertNotNull(communicationClient)
        assertEquals("", communicationClient.sessionId)
        assertFalse(communicationClient.isServiceConnected)
    }

    @Test
    fun testSendMessage() {
        val testMessage = "testMessage"
        val bundle = Bundle().apply { putString("MESSAGE", testMessage) }
        communicationClient.sendMessage(testMessage)
        assertEquals(testMessage, mockMessageService)
    }

//    @Test
//    fun testResetState() {
//        communicationClient.resetState()
//        assertFalse(communicationClient.hasSubmittedRequests)
//        assertFalse(communicationClient.hasQueuedRequests)
//        assertFalse(communicationClient.hasRequestJobs)
//    }

//    @Test
//    fun testTrackEvent() {
//        communicationClient.trackEvent(Event.SDK_CONNECTION_REQUEST_STARTED)
//        // Assert the tracker received the correct event
//    }
//
//    @Test
//    fun testUpdateSessionDuration() {
//        val newDuration = 14 * 24 * 3600L // 14 days
//        communicationClient.updateSessionDuration(newDuration)
//        // Assert the session duration has been updated
//    }
//
//    @Test
//    fun testClearSession() {
//        communicationClient.clearSession {
//            // Assert the session has been cleared
//        }
//        assertEquals("", communicationClient.sessionId)
//    }
//
//    @Test
//    fun testHandleMessage() {
//        val message = "{ \"type\": \"KEYS_EXCHANGED\", \"data\": \"test_data\" }"
//        communicationClient.handleMessage(message)
//        // Assert the message has been handled correctly
//    }
//
//    @Test
//    fun testSendRequest() {
//        val request = EthereumRequest("1", EthereumMethod.ETH_SEND_TRANSACTION.value)
//        communicationClient.sendRequest(request) {
//            // Assert the request has been processed
//        }
//    }
//
//    @Test
//    fun testBindService() {
//        communicationClient.bindService()
//        // Assert the service has been bound
//    }
//
//    @Test
//    fun testUnbindService() {
//        communicationClient.unbindService()
//        assertFalse(communicationClient.isServiceConnected)
//    }
//
//    @Test
//    fun testInitiateKeyExchange() {
//        communicationClient.initiateKeyExchange()
//        // Assert the key exchange has been initiated
//    }
//
//    @Test
//    fun testSendKeyExchangeMessage() {
//        val message = "test_message"
//        communicationClient.sendKeyExchangeMesage(message)
//        // Assert the key exchange message has been sent
//    }
}