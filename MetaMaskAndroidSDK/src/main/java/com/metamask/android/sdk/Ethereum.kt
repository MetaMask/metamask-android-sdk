package com.metamask.android.sdk

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.CompletableDeferred
import com.metamask.android.sdk.EthereumMethod.*
import java.util.UUID
import kotlin.Error

class Ethereum(context: Context, lifecycle: Lifecycle): EthereumEventCallback {
    var connected = false
    var chainId: String? = null
    var selectedAddress: String? = null
    private val appContext = context

    private val communicationClient = CommunicationClient(context, lifecycle, this)

    override fun updateAccount(account: String) {
        Logger.log("Ethereum: Selected account changed: $account")
        selectedAddress = account
    }

    override fun updateChainId(chainId: String) {
        Logger.log("Ethereum: ChainId changed: $chainId")
        this.chainId = chainId
    }

    fun connect(dapp: Dapp, callback: (Any?) -> Unit) {
        Logger.log("Ethereum: connecting...")
        communicationClient.dapp = dapp
        requestAccounts(callback)
    }

    fun disconnect() {
        Logger.log("Ethereum: disconnecting...")
        communicationClient.unbindService()
        connected = false
    }

    private fun requestAccounts(callback: (Any?) -> Unit) {
        Logger.log("Requesting accounts")
        connected = true

        val providerRequest = EthereumRequest(
            UUID.randomUUID().toString(),
            GETMETAMASKPROVIDERSTATE.value
        )

        sendRequest(providerRequest) {
        }

        val accountsRequest = EthereumRequest(
            UUID.randomUUID().toString(),
            ETHREQUESTACCOUNTS.value
        )
        Logger.log("Now requesting accounts $accountsRequest")
        sendRequest(accountsRequest, callback)
    }

    fun sendRequest(request: EthereumRequest, callback: (Any?) -> Unit) {
        Logger.log("Sending request $request")
        if (!connected && request.method == ETHREQUESTACCOUNTS.value) {
            Logger.log("Binding comm service...")
            communicationClient.bindService()
            return requestAccounts(callback)
        }

        communicationClient.sendRequest(request, callback)

        val authorise = requiresAuthorisation(request.method)

//        if (authorise) {
//            // open MM wallet
//            Logger.log("Opening metamask")
//            openMetaMask()
//        }
    }

    private fun openMetaMask() {
        val intent = Intent().apply {
            component = ComponentName("io.metamask", "io.metamask.MainActivity")
        }
        appContext.startActivity(intent)
    }

    private fun requiresAuthorisation(method: String): Boolean {
        return when (method) {
            ETHREQUESTACCOUNTS.value -> selectedAddress.isNullOrEmpty()
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