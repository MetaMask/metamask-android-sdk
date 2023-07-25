package io.metamask.androidsdk

import android.content.Context
import android.content.Intent
import android.net.Uri
import io.metamask.androidsdk.EthereumMethod.*
import java.util.*

class Ethereum private constructor(private val context: Context): EthereumEventCallback {
    var connected = false

    var chainId: String? = null
        private set
    var selectedAddress: String? = null
        private set

    private val communicationClient = CommunicationClient(context, this)
    private var serverBoundServiceStarted = false

    companion object {
        private var instance: Ethereum? = null
        private const val DEFAULT_SESSION_DURATION: Long = 7 * 24 * 3600 // 7 days default
        private var sessionLifetime: Long = DEFAULT_SESSION_DURATION

        fun getInstance(context: Context, sessionDuration: Long = DEFAULT_SESSION_DURATION): Ethereum {
            if (instance == null) {
                instance = Ethereum(context)
                sessionLifetime = sessionDuration
            }
            return instance as Ethereum
        }
    }

    override fun updateAccount(account: String) {
        Logger.log("Ethereum: Selected account changed")
        selectedAddress = account
    }

    override fun updateChainId(chainId: String) {
        Logger.log("Ethereum: ChainId changed: $chainId")
        this.chainId = chainId
    }

    fun setSessionDuration(duration: Long) {
        sessionLifetime = duration
        communicationClient.setSessionDuration(duration)
    }

    fun clearSession() {
        communicationClient.clearSession()
    }

    fun connect(dapp: Dapp, callback: (Any?) -> Unit) {
        Logger.log("Ethereum: connecting...")
        communicationClient.setSessionDuration(sessionLifetime)
        communicationClient.trackEvent(Event.CONNECTIONREQUEST, null)
        communicationClient.dapp = dapp
        requestAccounts(callback)
    }

    fun disconnect() {
        Logger.log("Ethereum: disconnecting...")
        communicationClient.unbindService()
        connected = false
    }

    private fun requestAccounts(callback: (Any?) -> Unit) {
        Logger.log("Requesting ethereum accounts")

        connected = true

        val accountsRequest = EthereumRequest(
            UUID.randomUUID().toString(),
            ETHREQUESTACCOUNTS.value,
            ""
        )

        sendRequest(accountsRequest, callback)
    }

    fun sendRequest(request: EthereumRequest, callback: (Any?) -> Unit) {
        Logger.log("Sending request $request")
        if (!communicationClient.isServiceConnected) {
            communicationClient.bindService()
        }

        if (!connected && request.method == ETHREQUESTACCOUNTS.value) {
            return requestAccounts(callback)
        }

        communicationClient.sendRequest(request, callback)

        val authorise = requiresAuthorisation(request.method)

        if (authorise) {
            openMetaMask()
        }
    }

    private fun openMetaMask() {
        val deeplinkUrl = if (serverBoundServiceStarted) {
            "https://metamask.app.link"
        } else {
            serverBoundServiceStarted = true
            "https://metamask.app.link/bind"
        }

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(deeplinkUrl))
        context.startActivity(intent)
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