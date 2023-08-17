package io.metamask.androidsdk

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.scopes.ViewModelScoped
import java.util.*
import javax.inject.Inject

@HiltViewModel
class EthereumViewModel @Inject constructor (
    private val applicationRepository: ApplicationRepository
    ) : ViewModel(), EthereumEventCallback {

    private var connectRequestSent = false
    private val communicationClient = CommunicationClient(applicationRepository.context, this)
    private val _ethereumState: MutableLiveData<EthereumState> = MutableLiveData(EthereumState("", "", ""))

    // Ethereum LiveData
    val ethereumState: LiveData<EthereumState> get() = _ethereumState

    // Expose plain variables for developers who prefer not using observing love data
    var chainId = String()
        private set
    var selectedAddress = String()
        private set

    // Toggle SDK connection status tracking
    var enableDebug: Boolean = true
        set(value) {
            field = value
            communicationClient.enableDebug = value
        }

    init {
        _ethereumState.postValue(_ethereumState.value?.copy(
            selectedAddress = "",
            chainId = "",
            sessionId = getSessionId()
        ))
    }

    companion object {
        private const val METAMASK_DEEPLINK = "https://metamask.app.link"
        private const val METAMASK_BIND_DEEPLINK = "$METAMASK_DEEPLINK/bind"

        private const val DEFAULT_SESSION_DURATION: Long = 7 * 24 * 3600 // 7 days default
    }

    private var sessionLifetime: Long = DEFAULT_SESSION_DURATION

    override fun updateAccount(account: String) {
        Logger.log("Ethereum: Selected account changed")
        selectedAddress = account
        _ethereumState.postValue(
            _ethereumState.value?.copy(selectedAddress = account)
        )
    }

    override fun updateChainId(newChainId: String) {
        Logger.log("Ethereum: ChainId changed: $newChainId")
        chainId = newChainId
        _ethereumState.postValue(
            _ethereumState.value?.copy(chainId = newChainId)
        )
    }

    // Set session duration in seconds
    fun setSessionDuration(duration: Long) {
        sessionLifetime = duration
        communicationClient.setSessionDuration(duration)
    }

    // Clear persisted session. Subsequent MetaMask connection request will need approval
    fun clearSession() {
        connectRequestSent = false
        communicationClient.clearSession()
        selectedAddress = ""
        _ethereumState.postValue(
            _ethereumState.value?.copy(
                selectedAddress = "",
                chainId = "",
                sessionId = getSessionId()
            )
        )
    }

    fun getSessionId(): String = communicationClient.sessionId

    fun connect(dapp: Dapp, callback: ((Any?) -> Unit)? = null) {
        Logger.log("Ethereum: connecting...")
        connectRequestSent = true
        communicationClient.setSessionDuration(sessionLifetime)
        communicationClient.trackEvent(Event.CONNECTIONREQUEST, null)
        communicationClient.dapp = dapp

        requestAccounts(callback)
    }

    fun disconnect() {
        Logger.log("Ethereum: disconnecting...")

        connectRequestSent = false
        selectedAddress = ""
        _ethereumState.postValue(
            _ethereumState.value?.copy(
                selectedAddress = "",
                chainId = ""
            )
        )
        communicationClient.unbindService()
    }

    private fun requestAccounts(callback: ((Any?) -> Unit)? = null) {
        Logger.log("Requesting ethereum accounts")

        val providerStateRequest = EthereumRequest(
            UUID.randomUUID().toString(),
            EthereumMethod.GET_METAMASK_PROVIDER_STATE.value,
            ""
        )
        sendRequest(providerStateRequest)

        val accountsRequest = EthereumRequest(
            UUID.randomUUID().toString(),
            EthereumMethod.ETH_REQUEST_ACCOUNTS.value,
            ""
        )
        sendRequest(accountsRequest) {
            callback?.invoke(it)
        }
    }

    fun sendRequest(request: EthereumRequest, callback: ((Any?) -> Unit)? = null) {
        Logger.log("Sending request $request")

        if (!connectRequestSent) {
            requestAccounts {
                sendRequest(request, callback)
            }
            return
        }

        communicationClient.sendRequest(request) { response ->
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
        applicationRepository.context.startActivity(intent)
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