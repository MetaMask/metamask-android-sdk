package io.metamask.androidsdk

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.lang.ref.WeakReference

private const val METAMASK_DEEPLINK = "https://metamask.app.link"
private const val METAMASK_BIND_DEEPLINK = "$METAMASK_DEEPLINK/bind"
private const val DEFAULT_SESSION_DURATION: Long = 7 * 24 * 3600 // 7 days default

class Ethereum (
    private val context: Context,
    private val dappMetadata: DappMetadata,
    sdkOptions: SDKOptions? = null): EthereumEventCallback {
    private var connectRequestSent = false
    private val communicationClient: CommunicationClient? by lazy {
        CommunicationClient(context, null)
    }

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

    fun enableDebug(enable: Boolean) = apply {
        this.enableDebug = enable
    }

    private var sessionDuration: Long = DEFAULT_SESSION_DURATION

    override fun updateAccount(account: String) {
        Logger.log("Ethereum:: Selected account changed: $account")
        _ethereumState.postValue(
            currentEthereumState.copy(
                selectedAddress = account,
                sessionId = communicationClient?.sessionId ?: ""
            )
        )
    }

    override fun updateChainId(newChainId: String) {
        Logger.log("Ethereum:: ChainId changed: $newChainId")
        _ethereumState.postValue(
            currentEthereumState.copy(
                chainId = newChainId,
                sessionId = communicationClient?.sessionId ?: ""
            )
        )
    }

    // Set session duration in seconds
    fun updateSessionDuration(duration: Long = DEFAULT_SESSION_DURATION) = apply {
        sessionDuration = duration
        communicationClient?.updateSessionDuration(duration)
    }

    // Clear persisted session. Subsequent MetaMask connection request will need approval
    fun clearSession() {
        disconnect(true)
    }

    fun connect(callback: ((Result) -> Unit)? = null) {
        val error = dappMetadata.validationError
        if (error != null) {
            callback?.invoke((Result.Error(error)))
            return
        }

        Logger.log("Ethereum:: connecting...")
        connectRequestSent = true
        communicationClient?.dappMetadata = dappMetadata
        communicationClient?.ethereumEventCallbackRef = WeakReference(this)
        communicationClient?.updateSessionDuration(sessionDuration)
        communicationClient?.trackEvent(Event.SDK_CONNECTION_REQUEST_STARTED, null)

        _ethereumState.postValue(
            currentEthereumState.copy(
                selectedAddress = "",
                chainId = ""
            )
        )
        requestAccounts(callback)
    }

    fun connectWith(request: EthereumRequest, callback: ((Result) -> Unit)? = null) {
        Logger.log("Ethereum:: connecting with ${request.method}...")
        connectRequestSent = true
        communicationClient?.dappMetadata = dappMetadata
        communicationClient?.ethereumEventCallbackRef = WeakReference(this)
        communicationClient?.updateSessionDuration(sessionDuration)
        communicationClient?.trackEvent(Event.SDK_CONNECTION_REQUEST_STARTED, null)

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
        communicationClient?.trackEvent(Event.SDK_CONNECTION_REQUEST_STARTED, null)

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

    private fun sendConnectRequest(request: EthereumRequest, callback: ((Result) -> Unit)?) {
        sendRequest(request) { result ->
            when (result) {
                is Result.Error -> {
                    communicationClient?.trackEvent(Event.SDK_CONNECTION_FAILED, null)

                    if (
                        result.error.code == ErrorType.USER_REJECTED_REQUEST.code ||
                        result.error.code == ErrorType.UNAUTHORISED_REQUEST.code
                    ) {
                        communicationClient?.trackEvent(Event.SDK_CONNECTION_REJECTED, null)
                    }
                }
                is Result.Success -> {
                    communicationClient?.trackEvent(Event.SDK_CONNECTION_AUTHORIZED, null)
                }
            }
            callback?.invoke(result)
        }
    }

    fun disconnect(clearSession: Boolean = false) {
        Logger.log("Ethereum:: disconnecting...")
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
        Logger.log("Ethereum:: Requesting ethereum accounts")

        val accountsRequest = EthereumRequest(
            method = EthereumMethod.ETH_REQUEST_ACCOUNTS.value
        )
        sendConnectRequest(accountsRequest, callback)
        requestChainId()
    }

    fun sendRequest(request: RpcRequest, callback: ((Result) -> Unit)? = null) {
        Logger.log("Ethereum:: Sending request $request")
        if (!connectRequestSent) {
            requestAccounts {
                sendRequest(request, callback)
            }
            return
        }

        if (EthereumMethod.isReadOnly(request.method) && infuraProvider?.supportsChain(chainId) == true) {
            Logger.log("Ethereum:: Using Infura API for method ${request.method} on chain $chainId")
            infuraProvider.makeRequest(request, chainId, dappMetadata, callback)
        } else {
            communicationClient?.sendRequest(request) { response ->
                callback?.invoke(response)
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