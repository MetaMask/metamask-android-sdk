package com.metamask.android.sdk

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.CompletableDeferred
import com.metamask.android.sdk.EthereumMethod.*

class Ethereum(context: Context, lifecycle: Lifecycle): EthereumEventCallback {
    var connected = false
    var chainId: String? = null
    var selectedAddress: String? = null
    private val appContext = context

    private val communicationClient = CommunicationClient(context, lifecycle, this)

    override fun updateAccount(account: String) {
        Logger.log("Selected account changed: $chainId")
        selectedAddress = account
    }

    override fun updateChainId(chainId: String) {
        Logger.log("ChainId changed: $chainId")
        this.chainId = chainId
    }

    suspend fun connect(dapp: Dapp): String {
        Logger.log("Ethereum connecting")
        communicationClient.dapp = dapp
        return requestAccounts()
    }

    fun disconnect() {
        Logger.log("Ethereum disconnecting")
        communicationClient.unbindService()
        connected = false
    }

    private suspend fun requestAccounts(): String {
        connected = true
        val providerRequest = EthereumRequest(
            communicationClient.sessionId,
            GETMETAMASKPROVIDERSTATE.name
        )
        sendRequest(providerRequest)

        val accountsRequest = EthereumRequest(
            communicationClient.sessionId,
            ETHREQUESTACCOUNTS.name
        )

        return sendRequest(accountsRequest) as? String ?: String()
    }

    suspend fun sendRequest(request: EthereumRequest): Any {
        Logger.log("Sending request! $request")
        if (!connected && request.method == EthereumMethod.ETHREQUESTACCOUNTS.value) {
            communicationClient.bindService()
            return requestAccounts()
        }

        val deferred = CompletableDeferred<Any>()
        communicationClient.sendRequest(request, deferred)

        val authorise = requiresAuthorisation(request.method)

        if (authorise) {
            // open MM wallet
            val overlayServiceIntent = Intent(appContext, PartialOverlayService::class.java)
            ContextCompat.startForegroundService(appContext, overlayServiceIntent)
        }

        return deferred.await()
    }

    private fun requiresAuthorisation(method: String): Boolean {
        return when (method) {
            ETHREQUESTACCOUNTS.name -> selectedAddress.isNullOrEmpty()
            else -> {
                if (EthereumMethod.hasMethod(method)) {
                    return EthereumMethod.requiresAuthorisation(method)
                }

                when(connected) {
                    true -> false
                    else -> true
                }
            }
        }
    }
}