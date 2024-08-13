package io.metamask.androidsdk

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.os.IBinder
import io.metamask.androidsdk.KeyExchangeMessageType.*
import io.metamask.nativesdk.IMessegeService
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.mock
import org.mockito.kotlin.any
import org.mockito.Mockito

import org.robolectric.RobolectricTestRunner
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

@RunWith(RobolectricTestRunner::class)
class CommunicationClientTest {

    private lateinit var context: Context

    private lateinit var mockEthereumEventCallback: EthereumEventCallback
    private lateinit var logger: Logger
    private lateinit var keyExchange: KeyExchange
    private lateinit var sessionManager: SessionManager
    private lateinit var mockClientServiceConnection: MockClientServiceConnection
    private lateinit var mockClientMessageServiceCallback: MockClientMessageServiceCallback
    private lateinit var communicationClient: CommunicationClient
    private lateinit var mockCrypto: MockCrypto

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        context = mock()

        logger = TestLogger
        mockEthereumEventCallback = MockEthereumEventCallback()
        mockClientServiceConnection = MockClientServiceConnection()
        mockClientMessageServiceCallback = MockClientMessageServiceCallback()

        mockCrypto = MockCrypto()
        keyExchange = KeyExchange(mockCrypto, logger)
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
    fun testBindServiceCalledWhenServiceNotConnected() {
        assertFalse(mockClientServiceConnection.serviceConectionCalled)
        assertFalse(mockClientServiceConnection.registerCallbackCalled)
        assertFalse(communicationClient.isServiceConnected)

        val request = EthereumRequest(method = EthereumMethod.ETH_REQUEST_ACCOUNTS.value)

        communicationClient.sendRequest(request) { }
        assertTrue(communicationClient.requestedBindService)
        verify(context, times(1)).bindService(any(), any(), eq(Context.BIND_AUTO_CREATE))
    }

    @Test
    fun testSendRequestBeforeServiceConnection() {
        assertFalse(mockClientServiceConnection.serviceConectionCalled)
        assertFalse(mockClientServiceConnection.registerCallbackCalled)
        assertFalse(communicationClient.isServiceConnected)

        val request = EthereumRequest(method = EthereumMethod.ETH_REQUEST_ACCOUNTS.value)
        communicationClient.sendRequest(request) { }

        // check that bindService is called
        assertTrue(communicationClient.requestedBindService)

        assertTrue(communicationClient.queuedRequests.isNotEmpty())
        assertTrue(communicationClient.requestJobs.isNotEmpty())

        assertFalse(mockClientServiceConnection.sendMessageCalled)
    }

    @Test
    fun testSendRequestAfterServiceConnectionBeforeKeysExchangeInitiatesKeyExchange() {
        val mockBinder = Mockito.mock(IBinder::class.java)
        val mockMessageService = Mockito.mock(IMessegeService::class.java)
        `when`(IMessegeService.Stub.asInterface(mockBinder)).thenReturn(mockMessageService)

        mockClientServiceConnection.onServiceConnected(ComponentName(context, "Service"), mockBinder)
        assertTrue(communicationClient.isServiceConnected)

        val request = EthereumRequest(method = EthereumMethod.ETH_REQUEST_ACCOUNTS.value)
        communicationClient.sendRequest(request) { }

        assertTrue(mockClientServiceConnection.sendMessageCalled)

        keyExchange.reset()

        // test that sent message is key exchange
        val sentMessage = mockClientServiceConnection.sentMessage
        assertNotNull(sentMessage)
        
        val keyExchangeJsonString = sentMessage?.getString(KEY_EXCHANGE) ?: ""
        val keyExchangeJsonObject = JSONObject(keyExchangeJsonString)
        assertEquals(keyExchangeJsonObject.getString(KeyExchange.TYPE), KeyExchangeMessageType.KEY_HANDSHAKE_SYN.name)
        assertEquals(keyExchangeJsonObject.getString(KeyExchange.PUBLIC_KEY), keyExchange.publicKey)
    }

    @Test
    fun testSendMessageBeforeMetamaskIsReadySendsOriginatorInfo() {
        val mockBinder = Mockito.mock(IBinder::class.java)
        val mockMessageService = Mockito.mock(IMessegeService::class.java)
        `when`(IMessegeService.Stub.asInterface(mockBinder)).thenReturn(mockMessageService)

        // mock service connection
        mockClientServiceConnection.onServiceConnected(ComponentName(context, "Service"), mockBinder)

        // mock receiver
        val receiverKeyExchange = KeyExchange(MockCrypto(), logger)

        // exchange public keys
        val receiverKeyExchangeMessage = KeyExchangeMessage(KEY_HANDSHAKE_ACK.name, receiverKeyExchange.publicKey)
        val senderKeyExchangeMessage = KeyExchangeMessage(KEY_HANDSHAKE_ACK.name, keyExchange.publicKey)

        keyExchange.nextKeyExchangeMessage(receiverKeyExchangeMessage)
        receiverKeyExchange.nextKeyExchangeMessage(senderKeyExchangeMessage)

        // mock key exchange complete
        keyExchange.complete()

        val request = EthereumRequest(method = EthereumMethod.ETH_REQUEST_ACCOUNTS.value)
        communicationClient.sendRequest(request) { }

        // test that message sent message is OriginatorInfo
        val sentMessageBundle = mockClientServiceConnection.sentMessage
        val sentMessageJsonString = sentMessageBundle?.getString(MESSAGE) ?: ""

        val messageJsonObject = JSONObject(sentMessageJsonString)
        val encryptedMessage = messageJsonObject.getString(MESSAGE)
        val decryptedMessage = receiverKeyExchange.decrypt(encryptedMessage)
        val messageJSON = JSONObject(decryptedMessage)
        assertEquals(messageJSON.getString("type"), "originator_info")
    }

    @Test
    fun testSendRequestMessageWhenMetaMaskIsReady() {
        val mockBinder = Mockito.mock(IBinder::class.java)
        val mockMessageService = Mockito.mock(IMessegeService::class.java)
        `when`(IMessegeService.Stub.asInterface(mockBinder)).thenReturn(mockMessageService)

        // mock service connection
        mockClientServiceConnection.onServiceConnected(ComponentName(context, "Service"), mockBinder)

        // mock receiver
        val receiverKeyExchange = KeyExchange(MockCrypto(), logger)

        // exchange public keys
        val receiverKeyExchangeMessage = KeyExchangeMessage(KEY_HANDSHAKE_ACK.name, receiverKeyExchange.publicKey)
        val senderKeyExchangeMessage = KeyExchangeMessage(KEY_HANDSHAKE_ACK.name, keyExchange.publicKey)

        keyExchange.nextKeyExchangeMessage(receiverKeyExchangeMessage)
        receiverKeyExchange.nextKeyExchangeMessage(senderKeyExchangeMessage)

        // mock key exchange complete
        keyExchange.complete()

        val request = EthereumRequest(method = EthereumMethod.ETH_REQUEST_ACCOUNTS.value)
        communicationClient.sendRequest(request) { }

        // mock receiving ready message
        val readyMessage = JSONObject().apply {
            put(MessageType.TYPE.value, MessageType.READY.value)
        }.toString()
        val encryptedReadyMessage = receiverKeyExchange.encrypt(readyMessage)

        // simulate MetaMask Ready
        communicationClient.handleMessage(encryptedReadyMessage)

        // test that message sent message is request message
        val sentMessageBundle = mockClientServiceConnection.sentMessage
        val sentMessageJsonString = sentMessageBundle?.getString(MESSAGE) ?: ""

        val messageJsonObject = JSONObject(sentMessageJsonString)
        val encryptedMessage = messageJsonObject.getString(MESSAGE)
        val decryptedMessage = receiverKeyExchange.decrypt(encryptedMessage)
        val messageJSON = JSONObject(decryptedMessage)
        assertEquals(messageJSON.getString("method"), request.method)
        assertEquals(messageJSON.getString("id"), request.id)
    }
}