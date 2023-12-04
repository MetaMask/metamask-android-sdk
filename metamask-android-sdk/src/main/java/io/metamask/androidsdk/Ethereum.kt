package io.metamask.androidsdk

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import java.lang.ref.WeakReference
import java.util.*

class Ethereum (private val context: Context): EthereumEventCallback {
    private var connectRequestSent = false
    private var communicationClient: CommunicationClient? = CommunicationClient(context, null)

    // Ethereum LiveData
    private val _ethereumState: MutableLiveData<EthereumState> = MutableLiveData(EthereumState("", "", ""))
    val ethereumState: MutableLiveData<EthereumState> get() = _ethereumState

    // Expose plain variables for developers who prefer not using observing live data via ethereumState
    var chainId: String = ""
        private set
    var selectedAddress = ""
        private set

    // Toggle SDK tracking
    var enableDebug: Boolean = true
        set(value) {
            field = value
            communicationClient?.enableDebug = value
        }

    fun enableDebug(enable: Boolean) = apply {
        this.enableDebug = enable
    }

    companion object {
        private const val METAMASK_DEEPLINK = "https://metamask.app.link"
        private const val METAMASK_BIND_DEEPLINK = "$METAMASK_DEEPLINK/bind"
        private const val DEFAULT_SESSION_DURATION: Long = 7 * 24 * 3600 // 7 days default
    }

    private var sessionDuration: Long = DEFAULT_SESSION_DURATION

    override fun updateAccount(account: String) {
        Logger.log("Ethereum:: Selected account changed: $account")
        _ethereumState.postValue(
            _ethereumState.value?.copy(
                selectedAddress = account
            )
        )
        selectedAddress = account
    }

    override fun updateChainId(newChainId: String) {
        Logger.log("Ethereum:: ChainId changed: $newChainId")
        _ethereumState.postValue(
            _ethereumState.value?.copy(
                chainId = newChainId
            )
        )
        chainId = newChainId
    }

    // Set session duration in seconds
    fun updateSessionDuration(duration: Long = DEFAULT_SESSION_DURATION) = apply {
        this.sessionDuration = duration
        communicationClient?.updateSessionDuration(duration)
    }

    // Clear persisted session. Subsequent MetaMask connection request will need approval
    fun clearSession() {
        connectRequestSent = false
        communicationClient?.clearSession()
        _ethereumState.postValue(
            _ethereumState.value?.copy(
                sessionId = getSessionId()
            )
        )
    }

    private fun getSessionId(): String = communicationClient?.sessionId ?: ""

    fun connect(dapp: Dapp, callback: ((Any?) -> Unit)? = null) {
        if (dapp.validationError != null) {
            callback?.invoke((dapp.validationError))
            return
        }

        Logger.log("Ethereum:: connecting...")
        communicationClient?.dapp = dapp
        connectRequestSent = true
        communicationClient?.ethereumEventCallbackRef = WeakReference(this)
        communicationClient?.updateSessionDuration(sessionDuration)
        communicationClient?.trackEvent(Event.SDK_CONNECTION_REQUEST_STARTED, null)

        _ethereumState.postValue(
            _ethereumState.value?.copy(
                selectedAddress = "",
                chainId = ""
            )
        )
        requestAccounts(callback)
    }

    fun disconnect() {
        Logger.log("Ethereum:: disconnecting...")

        connectRequestSent = false
        selectedAddress = ""
        _ethereumState.postValue(
            _ethereumState.value?.copy(
                selectedAddress = "",
                chainId = ""
            )
        )
        communicationClient?.unbindService()
    }

    private fun requestAccounts(callback: ((Any?) -> Unit)? = null) {
        Logger.log("Ethereum:: Requesting ethereum accounts")

        val accountsRequest = EthereumRequest(
            UUID.randomUUID().toString(),
            EthereumMethod.ETH_REQUEST_ACCOUNTS.value
        )
        sendRequest(accountsRequest) { result ->
            if (result is Result.Error) {
                communicationClient?.trackEvent(Event.SDK_CONNECTION_FAILED, null)

                if (
                    result.error.code == ErrorType.USER_REJECTED_REQUEST.code ||
                    result.error.code == ErrorType.UNAUTHORISED_REQUEST.code
                ) {
                    communicationClient?.trackEvent(Event.SDK_CONNECTION_REJECTED, null)
                }
            } else {
                communicationClient?.trackEvent(Event.SDK_CONNECTION_AUTHORIZED, null)
            }
            callback?.invoke(result)
        }
    }

    fun sendRequest(request: RpcRequest, callback: ((Result) -> Unit)? = null) {
        Logger.log("Ethereum:: Sending request $request")

        if (!connectRequestSent) {
            requestAccounts {
                sendRequest(request, callback)
            }
            return
        }

        communicationClient?.sendRequest(request) { response ->
            callback?.invoke(response)
        }

        val authorise = requiresAuthorisation(request.method)

        if (authorise) {
            openMetaMask()
        }
    }

    fun sendRequestBatch(requests: List<EthereumRequest>, callback: ((Result) -> Unit)? = null) {
        val batchRequest = BatchRequest(method = EthereumMethod.METAMASK_BATCH.value, params = requests)
        sendRequest(batchRequest, callback)
    }

    private fun openMetaMask() {
        val deeplinkUrl = METAMASK_BIND_DEEPLINK

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(deeplinkUrl))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun requiresAuthorisation(method: String): Boolean {
        return if (EthereumMethod.hasMethod(method)) {
            EthereumMethod.requiresAuthorisation(method)
        } else {
            when(connectRequestSent) {
                true -> false
                else -> true
            }
        }
    }
}