package io.metamask.androidsdk

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import io.metamask.nativesdk.IMessegeService
import io.metamask.nativesdk.IMessegeServiceCallback
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.mockito.Captor
import org.mockito.Mockito

import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

@RunWith(RobolectricTestRunner::class)
//@Config(manifest="../app/src/main/AndroidManifest.xml", sdk = [33])
class CommunicationClientTest {

    private lateinit var context: Context

    private lateinit var mockEthereumEventCallback: EthereumEventCallback
    private lateinit var logger: Logger
    private lateinit var keyExchange: KeyExchange
    private lateinit var sessionManager: SessionManager
    private lateinit var mockClientServiceConnection: MockClientServiceConnection
    private lateinit var mockClientMessageServiceCallback: MockClientMessageServiceCallback
    private lateinit var communicationClient: CommunicationClient

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        context = mock()

        logger = TestLogger
        mockEthereumEventCallback = MockEthereumEventCallback()
        mockClientServiceConnection = MockClientServiceConnection()
        mockClientMessageServiceCallback = MockClientMessageServiceCallback()

        keyExchange = KeyExchange(MockCrypto(), logger)
        sessionManager = SessionManager(MockKeyStorage())

        communicationClient = CommunicationClient(
            context,
            mockEthereumEventCallback,
            sessionManager,
            keyExchange,
            mockClientServiceConnection,
            mockClientMessageServiceCallback,
            logger
        )
    }

    @Test
    fun testInit() {
        assertNotNull(communicationClient)
        assertEquals(sessionManager.sessionId, communicationClient.sessionId)
    }

    @Test
    fun testServiceConnection() {
        val mockBinder = Mockito.mock(IBinder::class.java)
        val mockMessageService = Mockito.mock(IMessegeService::class.java)
        `when`(IMessegeService.Stub.asInterface(mockBinder)).thenReturn(mockMessageService)

        mockClientServiceConnection.onServiceConnected(ComponentName(context, "Service"), mockBinder)

        assertTrue(mockClientServiceConnection.serviceConectionCalled)
        assertTrue(mockClientServiceConnection.registerCallbackCalled)
        assertTrue(communicationClient.isServiceConnected)
        verify(mockMessageService).registerCallback(any())
    }

    @Test
    fun testSendMessageBeforeKeysExchanged() {
        val testMessage = "test_message"

        assertTrue(communicationClient.submittedRequests.isEmpty())
        assertTrue(communicationClient.requestJobs.isEmpty())
        assertTrue(communicationClient.queuedRequests.isEmpty())

        communicationClient.sendMessage(testMessage)

        assertTrue(communicationClient.submittedRequests.isEmpty())
        assertFalse(communicationClient.requestJobs.isEmpty())

        assertFalse(mockClientServiceConnection.sendMessageCalled)
    }

    @Test
    fun testSendMessageAfterKeysExchanged() {
        val testMessage = "test_message"

        assertTrue(communicationClient.submittedRequests.isEmpty())
        assertTrue(communicationClient.requestJobs.isEmpty())
        assertTrue(communicationClient.queuedRequests.isEmpty())

        // force key exchange
        keyExchange.complete()

        communicationClient.sendMessage(testMessage)

        assertTrue(mockClientServiceConnection.sendMessageCalled)
        assertEquals(testMessage, mockClientServiceConnection.sentMessage?.getString(MESSAGE))
    }

    @Test
    fun testMessageReception() {
        val testMessage = "test_message"

        val sentMessage = Bundle().apply {
            putString(MESSAGE, testMessage)
        }

        var receivedMessage: Bundle? = null

        // force key exchange
        keyExchange.complete()

        mockClientMessageServiceCallback.onMessage = { message ->
            receivedMessage = message
        }

        mockClientMessageServiceCallback.onMessageReceived(sentMessage)
        assertTrue(mockClientMessageServiceCallback.messageReceived)

        assertEquals(testMessage, receivedMessage?.getString(MESSAGE))
    }

    @Test
    fun testSendRequestBeforeServiceConnection() {
        assertFalse(mockClientServiceConnection.serviceConectionCalled)
        assertFalse(mockClientServiceConnection.registerCallbackCalled)
        assertFalse(communicationClient.isServiceConnected)

        val request = EthereumRequest(method = EthereumMethod.ETH_REQUEST_ACCOUNTS.value)
        communicationClient.sendRequest(request) { }
        assertTrue(communicationClient.queuedRequests.isNotEmpty())
        assertTrue(communicationClient.requestJobs.isNotEmpty())
        assertFalse(mockClientServiceConnection.sendMessageCalled)
    }
}