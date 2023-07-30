package io.metamask.androidsdk

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import java.lang.ref.WeakReference
import java.util.*

class EthereumViewModel(application: Application) : AndroidViewModel(application), EthereumEventCallback {

    private var connected = false
    private val mainHandler = Handler()
    private val appContextRef: WeakReference<Context> = WeakReference(application.applicationContext)
    private val communicationClient = CommunicationClient(application.applicationContext, this)

    // MutableLiveData for chainId and selectedAddress
    private val _chainId = MutableLiveData<String>()
    private val _selectedAddress = MutableLiveData<String>()

    // Expose immutable LiveData for chainId and selectedAddress to observe changes
    val activeChainId: MutableLiveData<String> get() = _chainId
    val activeAddress: MutableLiveData<String> get() = _selectedAddress

    // Expose plain variables for developers who prefer not using an observer pattern
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

    companion object {
        private const val METAMASK_DEEPLINK = "https://metamask.app.link"
        private const val METAMASK_BIND_DEEPLINK = "$METAMASK_DEEPLINK/bind"

        private const val DEFAULT_SESSION_DURATION: Long = 7 * 24 * 3600 // 7 days default
    }

    private var sessionLifetime: Long = DEFAULT_SESSION_DURATION

    override fun updateAccount(account: String) {
        Logger.log("Ethereum: Selected account changed")
        selectedAddress = account
        _selectedAddress.postValue(account)
    }

    override fun updateChainId(newChainId: String) {
        Logger.log("Ethereum: ChainId changed: $newChainId")
        chainId = newChainId
        _chainId.postValue(newChainId)
    }

    // Set session duration in seconds
    fun setSessionDuration(duration: Long) {
        sessionLifetime = duration
        communicationClient.setSessionDuration(duration)
    }

    // Clear persisted session. Subsequent MetaMask connection request will need approval
    fun clearSession() {
        connected = false
        _chainId.value = ""
        _selectedAddress.value = ""
        communicationClient.clearSession()
    }

    fun getSessionId(): String {
        return communicationClient.sessionId
    }

    fun connect(dapp: Dapp, callback: (Any?) -> Unit) {
        Logger.log("Ethereum: connecting...")
        connected = false
        communicationClient.setSessionDuration(sessionLifetime)
        communicationClient.trackEvent(Event.CONNECTIONREQUEST, null)
        communicationClient.dapp = dapp
        requestAccounts(callback)
    }

    fun disconnect() {
        Logger.log("Ethereum: disconnecting...")

        connected = false
        _chainId.value = ""
        _selectedAddress.value = ""
        communicationClient.unbindService()
    }

    private fun requestAccounts(callback: (Any?) -> Unit) {
        Logger.log("Requesting ethereum accounts")

        val accountsRequest = EthereumRequest(
            UUID.randomUUID().toString(),
            EthereumMethod.ETHREQUESTACCOUNTS.value,
            ""
        )

        sendRequest(accountsRequest) {
            connected = true
            callback(it)
        }
    }

    fun sendRequest(request: EthereumRequest, callback: (Any?) -> Unit) {
        Logger.log("Sending request $request")

        if (request.method != EthereumMethod.ETHREQUESTACCOUNTS.value && !connected) {
            requestAccounts {
                sendRequest(request, callback)
            }
            return
        }

        communicationClient.sendRequest(request) { response ->
            mainHandler.post {
                callback(response)
            }
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
        appContextRef.get()?.startActivity(intent)
    }

    private fun requiresAuthorisation(method: String): Boolean {
        return if (EthereumMethod.hasMethod(method)) {
            EthereumMethod.requiresAuthorisation(method)
        } else {
            when(connected) {
                true -> false
                else -> true
            }
        }
    }
}