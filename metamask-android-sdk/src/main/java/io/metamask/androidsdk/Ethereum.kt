package io.metamask.androidsdk

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

private const val METAMASK_DEEPLINK = "https://metamask.app.link"
private const val METAMASK_BIND_DEEPLINK = "$METAMASK_DEEPLINK/bind"

class Ethereum (
    private val context: Context,
    private val dappMetadata: DappMetadata,
    sdkOptions: SDKOptions? = null,
    private val logger: Logger = DefaultLogger,
    private val communicationClientModule: CommunicationClientModule = CommunicationClientModule(context)
    ): EthereumEventCallback {
    private var connectRequestSent = false

    private val communicationClient: CommunicationClient? by lazy {
        communicationClientModule.provideCommunicationClient(this)
    }

    private val storage = communicationClientModule.provideKeyStorage()
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val infuraProvider: InfuraProvider? = sdkOptions?.let {
        if (it.infuraAPIKey.isNotEmpty()) {
            InfuraProvider(it.infuraAPIKey)
        } else {
            null
        }
    }

    // Ethereum LiveData
    private val _ethereumState = MutableLiveData(EthereumState("", "", ""))
    private val currentEthereumState: EthereumState
        get() = checkNotNull(ethereumState.value)
    val ethereumState: LiveData<EthereumState> get() = _ethereumState

    // Expose plain variables for developers who prefer not using observing live data via ethereumState
    val chainId: String
        get() = currentEthereumState.chainId
    val selectedAddress: String
        get() = currentEthereumState.selectedAddress

    // Toggle SDK tracking
    var enableDebug: Boolean = true
        set(value) {
            field = value
            communicationClient?.enableDebug = value
        }

    init {
        updateSessionDuration()
        initializeEthereumState()
    }

    private fun initializeEthereumState() {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val account = storage.getValue(key = SessionManager.SESSION_ACCOUNT_KEY, file = SessionManager.SESSION_CONFIG_FILE)
                val chainId = storage.getValue(key = SessionManager.SESSION_CHAIN_ID_KEY, file = SessionManager.SESSION_CONFIG_FILE)
                _ethereumState.postValue(
                    currentEthereumState.copy(
                        selectedAddress = account ?: "",
                        chainId = chainId ?: ""
                    )
                )
            } catch (e: Exception) {
                logger.error(e.localizedMessage)
            }
        }
    }

    fun enableDebug(enable: Boolean) = apply {
        this.enableDebug = enable
    }

    private var sessionDuration: Long = SessionManager.DEFAULT_SESSION_DURATION

    override fun updateAccount(account: String) {
        logger.log("Ethereum:: Selected account changed: $account")
        _ethereumState.postValue(
            currentEthereumState.copy(
                selectedAddress = account,
                sessionId = communicationClient?.sessionId ?: ""
            )
        )
        if (account.isNotEmpty()) {
            storage.putValue(account, key = SessionManager.SESSION_ACCOUNT_KEY, SessionManager.SESSION_CONFIG_FILE)
        }
    }

    override fun updateChainId(newChainId: String) {
        logger.log("Ethereum:: ChainId changed: $newChainId")
        _ethereumState.postValue(
            currentEthereumState.copy(
                chainId = newChainId,
                sessionId = communicationClient?.sessionId ?: ""
            )
        )
        if (newChainId.isNotEmpty()) {
            storage.putValue(newChainId, key = SessionManager.SESSION_CHAIN_ID_KEY, SessionManager.SESSION_CONFIG_FILE)
        }
    }

    // Set session duration in seconds
    fun updateSessionDuration(duration: Long = SessionManager.DEFAULT_SESSION_DURATION) = apply {
        sessionDuration = duration
        communicationClient?.updateSessionDuration(duration)
    }

    // Clear persisted session. Subsequent MetaMask connection request will need approval
    fun clearSession() {
        disconnect(true)
        storage.clear(SessionManager.SESSION_CONFIG_FILE)
    }

    fun connect(callback: ((Result) -> Unit)? = null) {
        connectRequestSent = true
        val error = dappMetadata.validationError
        if (error != null) {
            callback?.invoke((Result.Error(error)))
            return
        }

        logger.log("Ethereum:: connecting...")
        communicationClient?.dappMetadata = dappMetadata
        communicationClient?.ethereumEventCallbackRef = WeakReference(this)
        communicationClient?.updateSessionDuration(sessionDuration)
        communicationClient?.trackEvent(Event.SDK_CONNECTION_REQUEST_STARTED)

        _ethereumState.postValue(
            currentEthereumState.copy(
                selectedAddress = "",
                chainId = ""
            )
        )
        requestAccounts(callback)
    }

    /*
    ===================
    Convenience Methods
    ===================
     */

    fun connectWith(request: EthereumRequest, callback: ((Result) -> Unit)? = null) {
        logger.log("Ethereum:: connecting with ${request.method}...")
        connectRequestSent = true
        communicationClient?.dappMetadata = dappMetadata
        communicationClient?.ethereumEventCallbackRef = WeakReference(this)
        communicationClient?.updateSessionDuration(sessionDuration)
        communicationClient?.trackEvent(Event.SDK_CONNECTION_REQUEST_STARTED)

        _ethereumState.postValue(
            currentEthereumState.copy(
                selectedAddress = "",
                chainId = ""
            )
        )
        val sendRequest: EthereumRequest = if (request.method == EthereumMethod.METAMASK_CONNECT_WITH.value) {
            request
        } else {
            EthereumRequest(
                method = EthereumMethod.METAMASK_CONNECT_WITH.value,
                params = listOf(request)
            )
        }

        sendConnectRequest(sendRequest, callback)
    }

    fun connectSign(message: String, callback: ((Result) -> Unit)? = null) {
        connectRequestSent = true
        communicationClient?.dappMetadata = dappMetadata
        communicationClient?.ethereumEventCallbackRef = WeakReference(this)
        communicationClient?.updateSessionDuration(sessionDuration)
        communicationClient?.trackEvent(Event.SDK_CONNECTION_REQUEST_STARTED)

        _ethereumState.postValue(
            currentEthereumState.copy(
                selectedAddress = "",
                chainId = ""
            )
        )

        val connectSignRequest = EthereumRequest(
            method = EthereumMethod.METAMASK_CONNECT_SIGN.value,
            params = listOf(message)
        )
        sendConnectRequest(connectSignRequest, callback)
    }

    private fun ethereumRequest(method: EthereumMethod, params: Any?, callback: ((Result) -> Unit)?) {
        sendRequest(
            EthereumRequest(method = method.value, params = params),
            callback
        )
    }

    fun getChainId(callback: ((Result) -> Unit)?) {
        ethereumRequest(method = EthereumMethod.ETH_CHAIN_ID, params = null, callback)
    }

    fun getEthAccounts(callback: ((Result) -> Unit)?) {
        ethereumRequest(method = EthereumMethod.ETH_ACCOUNTS, params = null, callback)
    }

    fun getEthBalance(address: String, block: String, callback: ((Result) -> Unit)? = null) {
        ethereumRequest(EthereumMethod.ETH_GET_BALANCE, params = listOf(address, block), callback)
    }

    fun getEthBlockNumber(callback: ((Result) -> Unit)?) {
        ethereumRequest(method = EthereumMethod.ETH_BLOCK_NUMBER, params = null, callback)
    }

    fun getEthEstimateGas(callback: ((Result) -> Unit)?) {
        ethereumRequest(method = EthereumMethod.ETH_ESTIMATE_GAS, params = null, callback)
    }

    fun getWeb3ClientVersion(callback: ((Result) -> Unit)?) {
        ethereumRequest(method = EthereumMethod.WEB3_CLIENT_VERSION, params = listOf<String>(), callback)
    }

    fun personalSign(message: String, address: String, callback: ((Result) -> Unit)?) {
        ethereumRequest(method = EthereumMethod.PERSONAL_SIGN, params = listOf(address, message), callback)
    }

    fun ethSignTypedDataV4(typedData: Any, address: String, callback: ((Result) -> Unit)?) {
        ethereumRequest(method = EthereumMethod.ETH_SIGN_TYPED_DATA_V4, params = listOf(address, typedData), callback)
    }

    fun sendTransaction(from: String, to: String, amount: String, callback: ((Result) -> Unit)?) {
        ethereumRequest(method = EthereumMethod.ETH_SEND_TRANSACTION, params = listOf(mutableMapOf(
            "from" to from,
            "to" to to,
            "amount" to amount
        )), callback)
    }

    fun sendRawTransaction(signedTransaction: String, callback: ((Result) -> Unit)?) {
        ethereumRequest(method = EthereumMethod.ETH_SEND_RAW_TRANSACTION, params = listOf(signedTransaction), callback)
    }

    fun getBlockTransactionCountByNumber(blockNumber: String, callback: ((Result) -> Unit)?) {
        ethereumRequest(method = EthereumMethod.ETH_GET_BLOCK_TRANSACTION_COUNT_BY_NUMBER, params = listOf(blockNumber), callback)
    }

    fun getBlockTransactionCountByHash(blockHash: String, callback: ((Result) -> Unit)?) {
        ethereumRequest(method = EthereumMethod.ETH_GET_BLOCK_TRANSACTION_COUNT_BY_HASH, params = listOf(blockHash), callback)
    }

    fun getTransactionCount(address: String, tagOrblockNumber: String, callback: ((Result) -> Unit)?) {
        ethereumRequest(method = EthereumMethod.ETH_GET_TRANSACTION_COUNT, params = listOf(address, tagOrblockNumber), callback)
    }

    fun addEthereumChain(targetChainId: String, rpcUrls: List<String>?, callback: ((Result) -> Unit)?) {
        ethereumRequest(method = EthereumMethod.ADD_ETHEREUM_CHAIN, params = listOf(mapOf(
            "chainId" to targetChainId,
            "chainName" to Network.chainNameFor(targetChainId),
            "rpcUrls" to (rpcUrls ?: Network.rpcUrls(Network.fromChainId(targetChainId)))
        )), callback)
    }

    fun switchEthereumChain(targetChainId: String, callback: ((Result) -> Unit)?) {
        ethereumRequest(method = EthereumMethod.SWITCH_ETHEREUM_CHAIN, params = listOf(mapOf("chainId" to targetChainId)), callback)
    }

    private fun sendConnectRequest(request: EthereumRequest, callback: ((Result) -> Unit)?) {
        sendRequest(request) { result ->
            when (result) {
                is Result.Error -> {
                    communicationClient?.trackEvent(Event.SDK_CONNECTION_FAILED)

                    if (
                        result.error.code == ErrorType.USER_REJECTED_REQUEST.code ||
                        result.error.code == ErrorType.UNAUTHORISED_REQUEST.code
                    ) {
                        communicationClient?.trackEvent(Event.SDK_CONNECTION_REJECTED)
                    }
                }
                is Result.Success -> {
                    communicationClient?.trackEvent(Event.SDK_CONNECTION_AUTHORIZED)
                }
            }
            callback?.invoke(result)
        }
    }

    fun disconnect(clearSession: Boolean = false) {
        logger.log("Ethereum:: disconnecting...")
        connectRequestSent = false
        communicationClient?.resetState()
        communicationClient?.unbindService()

        if (clearSession) {
            communicationClient?.clearSession {
                resetEthereumState()
            }
        } else {
            resetEthereumState()
        }
    }

    private fun resetEthereumState() {
        _ethereumState.postValue(
            currentEthereumState.copy(
                selectedAddress = "",
                sessionId = communicationClient?.sessionId ?: "",
                chainId = ""
            )
        )
    }

    private fun requestChainId() {
        val chainIdRequest = EthereumRequest(
            method = EthereumMethod.ETH_CHAIN_ID.value
        )
        sendRequest(chainIdRequest)
    }

    private fun requestAccounts(callback: ((Result) -> Unit)? = null) {
        logger.log("Ethereum:: Requesting ethereum accounts")
        connectRequestSent = true

        val accountsRequest = EthereumRequest(
            method = EthereumMethod.ETH_REQUEST_ACCOUNTS.value
        )
        sendConnectRequest(accountsRequest, callback)
        requestChainId()
    }

    fun sendRequest(request: RpcRequest, callback: ((Result) -> Unit)? = null) {
        logger.log("Ethereum:: Sending request $request")

        if (!connectRequestSent && selectedAddress.isEmpty()) {
            requestAccounts {
                sendRequest(request, callback)
            }
            return
        }

        if (EthereumMethod.isReadOnly(request.method) && infuraProvider?.supportsChain(chainId) == true) {
            logger.log("Ethereum:: Using Infura API for method ${request.method} on chain $chainId")
            infuraProvider.makeRequest(request, chainId, dappMetadata, callback)
        } else {
            communicationClient?.sendRequest(request) { response ->
                callback?.invoke(response)
            }
            if (EthereumMethod.requiresAuthorisation(request.method)) {
                val params = mapOf(
                    "method" to request.method,
                    "from" to "mobile"
                )
                communicationClient?.trackEvent(Event.SDK_RPC_REQUEST, params)
            }
            openMetaMask()
        }
    }

    fun sendRequestBatch(requests: List<EthereumRequest>, callback: ((Result) -> Unit)? = null) {
        val batchRequest = AnyRequest(method = EthereumMethod.METAMASK_BATCH.value, params = requests)
        sendRequest(batchRequest, callback)
    }

    private fun openMetaMask() {
        val deeplinkUrl = METAMASK_BIND_DEEPLINK

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(deeplinkUrl))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}