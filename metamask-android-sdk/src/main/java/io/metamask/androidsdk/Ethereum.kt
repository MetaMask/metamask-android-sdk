package io.metamask.androidsdk

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import java.lang.ref.WeakReference
import java.util.*
import javax.inject.Inject

class Ethereum @Inject constructor (private val repository: ApplicationRepository): EthereumEventCallback {

    private var connectRequestSent = false
    private var communicationClient: CommunicationClient? = CommunicationClient(repository.context, null)

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
        Logger.log("Ethereum:: Selected account changed")
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
        Logger.log("Ethereum:: connecting...")
        connectRequestSent = true
        communicationClient?.ethereumEventCallbackRef = WeakReference(this)
        communicationClient?.updateSessionDuration(sessionDuration)
        communicationClient?.trackEvent(Event.SDK_CONNECTION_REQUEST_STARTED, null)
        communicationClient?.dapp = dapp

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

        val providerStateRequest = EthereumRequest(
            UUID.randomUUID().toString(),
            EthereumMethod.GET_METAMASK_PROVIDER_STATE.value,
            ""
        )
        sendRequest(providerStateRequest) { result ->
            if (result is RequestError) {
                Logger.error("Ethereum:: Provider Connection request failed ${result.message}")
                communicationClient?.trackEvent(Event.SDK_CONNECTION_FAILED, null)

                if (result.code == ErrorType.USER_REJECTED_REQUEST.code) {
                    Logger.error("Ethereum:: Provider Connection request rejected")
                    communicationClient?.trackEvent(Event.SDK_CONNECTION_REJECTED, null)
                }
            } else {
                communicationClient?.trackEvent(Event.SDK_CONNECTION_AUTHORIZED, null)
                updateSessionDuration()
            }
        }

        val accountsRequest = EthereumRequest(
            UUID.randomUUID().toString(),
            EthereumMethod.ETH_REQUEST_ACCOUNTS.value,
            ""
        )
        sendRequest(accountsRequest) { result ->
            if (result is RequestError) {
                communicationClient?.trackEvent(Event.SDK_CONNECTION_FAILED, null)

                if (
                    result.code == ErrorType.USER_REJECTED_REQUEST.code ||
                    result.code == ErrorType.UNAUTHORISED_REQUEST.code
                ) {
                    communicationClient?.trackEvent(Event.SDK_CONNECTION_REJECTED, null)
                }
            } else {
                communicationClient?.trackEvent(Event.SDK_CONNECTION_AUTHORIZED, null)
            }
            callback?.invoke(result)
        }
    }

    fun sendRequest(request: EthereumRequest, callback: ((Any?) -> Unit)? = null) {
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

    private fun openMetaMask() {
        val deeplinkUrl = METAMASK_BIND_DEEPLINK

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(deeplinkUrl))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        repository.context.startActivity(intent)
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