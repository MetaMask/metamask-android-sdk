
package io.metamask.androidsdk

import android.content.ComponentName
import android.content.Context
import android.os.IBinder
import io.metamask.androidsdk.KeyExchangeMessageType.*
import io.metamask.nativesdk.IMessegeService
import io.metamask.androidsdk.Event.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.mock
import org.mockito.Mockito

import org.robolectric.RobolectricTestRunner
import org.junit.runner.RunWith
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any

@RunWith(RobolectricTestRunner::class)
class EthereumTests {

    private lateinit var context: Context

    private lateinit var mockEthereumEventCallback: MockEthereumEventCallback
    private lateinit var logger: Logger
    private lateinit var keyExchange: KeyExchange
    private lateinit var sessionManager: SessionManager
    private lateinit var mockClientServiceConnection: MockClientServiceConnection
    private lateinit var mockClientMessageServiceCallback: MockClientMessageServiceCallback
    private lateinit var mockCrypto: MockCrypto
    private lateinit var mockTracker: MockTracker

    private lateinit var mockCommunicationClientModule: MockCommunicationClientModule
    private lateinit var ethereum: Ethereum
    private lateinit var mockStorage: MockKeyStorage
    private lateinit var communicationClient: CommunicationClient
    private lateinit var mockInfuraProvider: MockReadOnlyRPCProvider

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        context = mock()

        logger = TestLogger
        mockEthereumEventCallback = MockEthereumEventCallback()
        mockClientServiceConnection = MockClientServiceConnection()
        mockClientMessageServiceCallback = MockClientMessageServiceCallback()

        mockCrypto = MockCrypto()
        mockTracker = MockTracker()
        keyExchange = KeyExchange(mockCrypto, logger)
        mockStorage = MockKeyStorage()
        sessionManager = SessionManager(mockStorage)
        mockInfuraProvider = MockReadOnlyRPCProvider("01234567", null, logger)

        mockCommunicationClientModule = MockCommunicationClientModule(
            context,
            mockStorage,
            sessionManager,
            keyExchange,
            mockClientServiceConnection,
            mockClientMessageServiceCallback,
            mockTracker,
            logger
            )
        ethereum = Ethereum(
            context,
            DappMetadata("testApp","http://www.testapp.com", iconUrl = null, base64Icon = null),
            sdkOptions = SDKOptions(infuraAPIKey = "01234567", readonlyRPCMap = null),
            logger,
            mockCommunicationClientModule,
            mockInfuraProvider
        )
        communicationClient = ethereum.communicationClient!!
    }

    @Test
    fun testUpdateAccount() = runBlocking {
        val testAccount = "0x12345"
        ethereum.updateAccount(testAccount)
        delay(10)
        assertEquals(testAccount, ethereum.selectedAddress)
        assertEquals(testAccount, mockStorage.getValue(SessionManager.SESSION_ACCOUNT_KEY, SessionManager.SESSION_CONFIG_FILE))
    }

    @Test
    fun testUpdateChainId() = runBlocking {
        val testChainId = "0x1"
        ethereum.updateChainId(testChainId)
        delay(10)
        assertEquals(testChainId, ethereum.chainId)
        assertEquals(testChainId, mockStorage.getValue(SessionManager.SESSION_CHAIN_ID_KEY, SessionManager.SESSION_CONFIG_FILE))
    }

    @Test
    fun testEthereumConnect() {
        val testResult: Result = Result.Success.Item("0x123456")
        var callbackResult: Result? = null

        prepareCommunicationClient()

        ethereum.connect { result ->
            callbackResult = result
        }

        val requestId = findRequestIdForAccountRequest(EthereumMethod.ETH_REQUEST_ACCOUNTS)
        communicationClient.completeRequest(requestId, testResult)

        assertTrue(callbackResult is Result.Success)
        assertEquals(callbackResult, testResult)

        val trackedEvent = mockTracker.trackedEvent
        assertEquals(trackedEvent, SDK_CONNECTION_AUTHORIZED)
        assertNotNull(mockTracker.trackedEventParams)
        assertEquals(SDK_CONNECTION_AUTHORIZED.value, mockTracker.trackedEventParams?.get("event"))
    }

    @Test
    fun testEthereumConnectError() {
        val errorCode = 4001
        val errorMessage = "User rejected request"
        val testResult: Result = Result.Error(RequestError(errorCode, errorMessage))
        var callbackResult: Result? = null

        prepareCommunicationClient()

        // Assuming the connect method modifies the internal state and captures results
        ethereum.connect { result ->
            callbackResult = result
        }

        // Simulate the completion of the request made by connect
        val requestId = findRequestIdForAccountRequest(EthereumMethod.ETH_REQUEST_ACCOUNTS)
        communicationClient.completeRequest(requestId, testResult)

        assertTrue(callbackResult is Result.Error)
        assertEquals(testResult, callbackResult)

        val trackedEvent = mockTracker.trackedEvent
        assertEquals(SDK_CONNECTION_REJECTED, trackedEvent)
        assertNotNull(mockTracker.trackedEventParams)
        assertEquals(SDK_CONNECTION_REJECTED.value, mockTracker.trackedEventParams?.get("event"))
    }    

        @Test
    fun testConnectWith() {
        val params: MutableMap<String, Any> = mutableMapOf(
            "from" to "0x12345",
            "to" to "0x98765",
            "amount" to "0x1"
        )

        val transactionRequest = EthereumRequest(
            method = EthereumMethod.ETH_SEND_TRANSACTION.value,
            params = listOf(params)
        )

        var callbackResult: Result? = null

        prepareCommunicationClient()

        ethereum.connectWith(transactionRequest) { result ->
            callbackResult = result
        }

        val requestId = findRequestIdForAccountRequest(EthereumMethod.METAMASK_CONNECT_WITH)
        val testResult: Result = Result.Success.Item("0x24680")
        communicationClient.completeRequest(requestId, testResult)

        assertTrue(callbackResult is Result.Success)
        assertEquals(callbackResult, testResult)

        val trackedEvent = mockTracker.trackedEvent
        assertEquals(trackedEvent, SDK_CONNECTION_AUTHORIZED)
        assertNotNull(mockTracker.trackedEventParams)
        assertEquals(SDK_CONNECTION_AUTHORIZED.value, mockTracker.trackedEventParams?.get("event"))
    }

    @Test
    fun testConnectSign() {
        val messageToSign = "Sign this message"
        var callbackResult: Result? = null
    
        prepareCommunicationClient()
    
        ethereum.connectSign(messageToSign) { result ->
            callbackResult = result
        }
    
        val requestId = findRequestIdForAccountRequest(EthereumMethod.METAMASK_CONNECT_SIGN)
        val testResult: Result = Result.Success.Item("0xdhjdheeeeeew")
        communicationClient.completeRequest(requestId, testResult)
    
        // Assertions to verify the correct handling
        assertTrue(callbackResult is Result.Success)
        assertEquals(callbackResult, testResult)
        
        val trackedEvent = mockTracker.trackedEvent
        assertEquals(Event.SDK_CONNECTION_AUTHORIZED, trackedEvent)
        assertNotNull(mockTracker.trackedEventParams)
        assertEquals(SDK_CONNECTION_AUTHORIZED.value, mockTracker.trackedEventParams?.get("event"))
    }

    @Test
    fun testUpdateSessionDuration() {
        val newDuration = 10 * 24 * 3600L // 10 days
        runBlocking {
            ethereum.updateSessionDuration(newDuration)
            delay(10)

            // Ensure session duration in session manager is updated
            assertEquals(newDuration, sessionManager.sessionDuration)
        }
    }

    @Test
    fun testClearSession() {
        mockStorage.putValue("0x1", key = SessionManager.SESSION_CHAIN_ID_KEY, SessionManager.SESSION_CONFIG_FILE)
        assertFalse(mockStorage.isClear())
        ethereum.clearSession()
        assertTrue(mockStorage.isClear())
    }

    @Test
    fun testMetaMaskOpenedForUserInteraction() {
        val request = EthereumRequest(method = EthereumMethod.ETH_SEND_TRANSACTION.value, params = listOf("to: '0x456', value: '1000'"))
        ethereum.connect {}

        ethereum.sendRequest(request)

        // Assuming `openMetaMask` does something observable like firing an intent
        verify(context, atLeastOnce()).startActivity(any())
    }

    @Test
    fun testReadOnlyRequestUsingInfura() {
        val request = EthereumRequest(method = EthereumMethod.ETH_GET_BALANCE.value, params = listOf("0x123", "latest"))
        val mockResponse = "{\"balance\": \"1000\"}"
        mockInfuraProvider.mockResponse = mockResponse

        ethereum.connect {}

        ethereum.sendRequest(request) { result ->
            assertTrue(result is Result.Success)
            when (result) {
                is Result.Success.Item -> {
                    assertEquals(mockResponse, result.value)
                }
                else -> {
                    fail("Result should be success")
                }
            }
        }
    }    

    private fun findRequestIdForAccountRequest(method: EthereumMethod): String {
        return communicationClient.submittedRequests.entries.find {
            it.value.request.method == method.value
        }?.key ?: throw IllegalStateException("No account request found")
    }

    private fun prepareCommunicationClient() {
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

        // mock receiving ready message
        val readyMessage = JSONObject().apply {
            put(MessageType.TYPE.value, MessageType.READY.value)
        }.toString()
        val encryptedReadyMessage = receiverKeyExchange.encrypt(readyMessage)

        // simulate MetaMask Ready
        communicationClient.handleMessage(encryptedReadyMessage)
    }
}