package com.metamask.android.sdk

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.lifecycle.Lifecycle
import com.metamask.android.sdk.EthereumMethod.*
import java.util.UUID

class Ethereum private constructor(private val context: Context, private val lifecycle: Lifecycle): EthereumEventCallback {
    var connected = false

    var chainId: String? = null
        private set
    var selectedAddress: String? = null
        private set

    private val communicationClient = CommunicationClient(context, lifecycle, this)

    companion object {
        private var instance: Ethereum? = null

        fun getInstance(context: Context, lifecycle: Lifecycle): Ethereum {
            if (instance == null) {
                instance = Ethereum(context.applicationContext, lifecycle)
            }
            return instance as Ethereum
        }
    }

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
        val intent = Intent().apply {
            component = ComponentName("io.metamask", "io.metamask.MainActivity")
        }
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