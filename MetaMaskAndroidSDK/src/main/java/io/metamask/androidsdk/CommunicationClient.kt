package io.metamask.androidsdk

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.metamask.nativesdk.IMessegeService
import io.metamask.nativesdk.IMessegeServiceCallback
import kotlinx.serialization.Serializable
import org.json.JSONObject
import java.lang.ref.WeakReference

internal class CommunicationClient(context: Context, callback: EthereumEventCallback)  {

    var sessionId: String
    private val keyExchange: KeyExchange = KeyExchange()

    var dapp: Dapp? = null
    var isServiceConnected = false
        private set

    private val tracker: Tracker = Analytics()

    private var messageService: IMessegeService? = null
    private val appContextRef: WeakReference<Context> = WeakReference(context)
    private val ethereumEventCallbackRef: WeakReference<EthereumEventCallback> = WeakReference(callback)

    private var requestJobs: MutableList<() -> Unit> = mutableListOf()
    private var submittedRequests: MutableMap<String, SubmittedRequest>  = mutableMapOf()

    private var sessionManager: SessionManager

    private var isMetaMaskReady = false

    var enableDebug: Boolean = false
        set(value) {
        field = value
        tracker.enableDebug = value
    }

    init {
        sessionManager = SessionManager(KeyStorage(context))
        sessionId = sessionManager.sessionId
        Logger.log("CommunicationClient:: sessionId: $sessionId")
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            messageService = IMessegeService.Stub.asInterface(service)
            messageService?.registerCallback(messageServiceCallback)
            isServiceConnected = true
            Log.d(TAG,"CommunicationClient:: Service connected $name")
            initiateKeyExchange()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            messageService = null
            isServiceConnected = false
            Log.e(TAG,"CommunicationClient:: Service disconnected $name")
            trackEvent(Event.DISCONNECTED, null)
        }

        override fun onBindingDied(name: ComponentName?) {
            Logger.log("CommunicationClient:: binding died: $name")
        }

        override fun onNullBinding(name: ComponentName?) {
            Logger.log("CommunicationClient:: null binding: $name")
        }
    }

    private val messageServiceCallback: IMessegeServiceCallback = object : IMessegeServiceCallback.Stub() {
        override fun onMessageReceived(message: Bundle) {
            Logger.log("CommunicationClient:: onMessageReceived!")

            val keyExchange = message.getString(KEY_EXCHANGE)
            val message = message.getString(MESSAGE)

            if (keyExchange != null) {
                handleKeyExchange(keyExchange)
            } else if (message != null) {
                handleMessage(message)
            }
        }
    }

    fun trackEvent(event: Event, params: MutableMap<String, String>?) {
        val params: MutableMap<String, String> = params ?: mutableMapOf(
            "id" to sessionId
        )

        when(event) {
            Event.CONNECTIONREQUEST -> {
                params["commlayer"] = SDKInfo.PLATFORM
                params["sdkVersion"] = SDKInfo.VERSION
                params["url"] = dapp?.url ?: ""
                params["title"] = dapp?.name ?: ""
                params["platform"] = SDKInfo.PLATFORM
            }
            else -> Unit
        }
        tracker.trackEvent(event, params)
    }

    fun setSessionDuration(duration: Long) {
        sessionManager.setSessionDuration(duration)
    }

    fun clearSession() {
        Logger.log("Clearing current session")
        sessionManager.clearSession()
        sessionId = sessionManager.sessionId
        keyExchange.reset()
    }

    private fun handleMessage(message: String) {
        val jsonString = keyExchange.decrypt(message)
        Logger.log("CommunicationClient:: Received message: $jsonString")
        val json = JSONObject(jsonString)

        when (json.optString(MessageType.TYPE.value)) {
            MessageType.TERMINATE.value -> {
                Logger.log("CommunicationClient:: Connection terminated by MetaMask")
                unbindService()
                keyExchange.reset()
            }
            MessageType.PAUSE.value -> {
                Logger.log("CommunicationClient:: Connection paused")
            }
            MessageType.KEYS_EXCHANGED.value -> {
                Logger.log("CommunicationClient:: Keys exchanged")
                sendOriginatorInfo()
            }
            MessageType.READY.value -> {
                Logger.log("CommunicationClient:: Connection ready")
                isMetaMaskReady = true
                resumeRequestJobs()
            }
            MessageType.WALLET_INFO.value -> {
                Logger.log("CommunicationClient:: Received wallet info $json")
            }
            else -> {
                val data = json.optString(MessageType.DATA.value)

                Logger.log("CommunicationClient:: Received data $data")

                if (data.isNotEmpty()) {
                    val dataJson = JSONObject(data)
                    val id = dataJson.optString(MessageType.ID.value)

                    if (id.isNotEmpty()) {
                        handleResponse(id, dataJson)
                    } else if (dataJson.optString(MessageType.ERROR.value).isNotEmpty()) {
                        Logger.error("CommunicationClient:: Got error $dataJson")
                    } else {
                        handleEvent(dataJson)
                    }
                }
            }
        }
    }

    private fun resumeRequestJobs() {
        Logger.log("CommunicationClient:: Resuming jobs")

        while (requestJobs.isNotEmpty()) {
            Logger.log("CommunicationClient:: Running queued job")
            val job = requestJobs.removeFirstOrNull()
            job?.invoke()
        }
    }

    private fun addRequestJob(job: () -> Unit) {
        Logger.log("CommunicationClient:: Queued job")
        requestJobs.add(job)
    }

    private fun handleResponse(id: String, data: JSONObject) {
        // check for error
        Logger.log("CommunicationClient:: Got response: ${data}")
        val error = data.optString("error")

        if (handleError(error, id)) {
            return
        }

        val request = submittedRequests[id]?.request
        Logger.log("Response request type: $request")
        val isResultMethod = EthereumMethod.isResultMethod(request?.method ?: "")

        if (!isResultMethod) {
            val resultJson = data.optString("result")

            if (resultJson.isNotEmpty()) {
                val result: Map<String, Any?> = Gson().fromJson(resultJson, object : TypeToken<Map<String, Any?>>() {}.type)
                Logger.log("Result Map<String, Any?> is :$result")
                submittedRequests[id]?.callback?.invoke(result)
                completeRequest(id, result)
            } else {
                val result: Map<String, Serializable> = Gson().fromJson(data.toString(), object : TypeToken<Map<String, Serializable>>() {}.type)
                Logger.log("Result Map<String, Serializable> is :$result")
                completeRequest(id, result)
            }
            return
        }

        when(request?.method) {
            EthereumMethod.GETMETAMASKPROVIDERSTATE.value -> {
                val result = data.optString("result")
                Logger.log("metamask_getProviderState response: $result")
                Logger.log("Result is: $result")
                val resultJson = JSONObject(result)
                val accountsJson = resultJson.optString("accounts")
                val accounts: List<String> = Gson().fromJson(accountsJson, object : TypeToken<List<String>>() {}.type)

                val account = accounts.firstOrNull()

                if (account != null) {
                    updateAccount(account)
                    completeRequest(id, account)
                }

                val chainId = resultJson.optString("chainId")

                if (chainId.isNotEmpty()) {
                    updateChainId(chainId)
                    completeRequest(id, chainId)
                }
            }
            EthereumMethod.ETHREQUESTACCOUNTS.value  -> {
                val result = data.optString("result")
                val accounts: List<String> = Gson().fromJson(result, object : TypeToken<List<String>>() {}.type)
                val account = accounts.getOrNull(0)

                if (account != null) {
                    updateAccount(account)
                    completeRequest(id, account)
                } else {
                    Logger.error("CommunicationClient:: Request accounts failure")
                }
            }
            EthereumMethod.ETHCHAINID.value -> {
                val chainId = data.optString("result")

                if (chainId.isNotEmpty()) {
                    updateChainId(chainId)
                    completeRequest(id, chainId)
                }
            }
            EthereumMethod.ETHSIGNTYPEDDATAV3.value,
            EthereumMethod.ETHSIGNTYPEDDATAV4.value,
            EthereumMethod.ETHSENDTRANSACTION.value -> {
                val result = data.optString("result")

                if (result.isNotEmpty()) {
                    completeRequest(id, result)
                } else {
                    Logger.error("CommunicationClient:: Unexpected response: $data")
                }
            }
            else -> {
                val result = data.opt("result")
                if (result != null) {
                    completeRequest(id, result)
                } else {
                    Logger.error("Unexpected response: $data")
                }
            }
        }
    }

    private fun handleError(error: String, id: String): Boolean {
        if (error.isEmpty()) {
            return false
        }

        val error: Map<String, Any?> = Gson().fromJson(error, object : TypeToken<Map<String, Any?>>() {}.type)
        val errorCode = error["code"] as? Double ?: -1
        val code = errorCode.toInt()
        val message = error["message"] as? String ?: ErrorType.message(code)
        completeRequest(id, RequestError(code, message))
        return true
    }

    private fun completeRequest(id: String, result: Any) {
        submittedRequests[id]?.callback?.invoke(result)
        submittedRequests.remove(id)
    }

    private fun handleEvent(event: JSONObject) {
        Logger.log("CommunicationClient:: Got event: $event")

        when (event.optString("method")) {
            EthereumMethod.METAMASKACCOUNTSCHANGED.value -> {
                val accountsJson = event.optString("params")
                val accounts: List<String> = Gson().fromJson(accountsJson, object : TypeToken<List<String>>() {}.type)

                accounts.getOrNull(0)?.let { account ->
                    updateAccount(account)
                }
            }
            EthereumMethod.METAMASKCHAINCHANGED.value -> {
                val paramsJson = event.optJSONObject("params")
                val chainId = paramsJson?.optString("chainId")

                Logger.log("CommunicationClient:: Got metamask_chainChanged: params: $paramsJson, chainId: $chainId")

                if (chainId != null && chainId.isNotEmpty()) {
                    updateChainId(chainId)
                }
            }
            else -> {
                Logger.error("CommunicationClient:: Unexpected event: $event")
            }
        }
    }

    private fun updateAccount(account: String) {
        Logger.log("CommunicationClient:: Received account event: $account")
        val callback = ethereumEventCallbackRef.get()
        callback?.updateAccount(account)
    }

    private fun updateChainId(chainId: String) {
        Logger.log("CommunicationClient:: Received chain event: $chainId")
        val callback = ethereumEventCallbackRef.get()
        callback?.updateChainId(chainId)
    }

    private fun handleKeyExchange(message: String) {
        Logger.log("CommunicationClient:: Received key exchange $message")

        val json = JSONObject(message)

        val keyExchangeStep = json.optString(KeyExchange.TYPE, KeyExchangeMessageType.KEY_HANDSHAKE_SYN.name)
        val type = KeyExchangeMessageType.valueOf(keyExchangeStep)
        val theirPublicKey = json.optString(KeyExchange.PUBLIC_KEY)
        val keyExchangeMessage = KeyExchangeMessage(type.name, theirPublicKey)
        val nextStep  = keyExchange.nextKeyExchangeMessage(keyExchangeMessage)

        if (
            type == KeyExchangeMessageType.KEY_HANDSHAKE_SYNACK ||
            type == KeyExchangeMessageType.KEY_HANDSHAKE_ACK) {
            keyExchange.complete()
        }

        if (nextStep != null) {
            val exchangeMessage = JSONObject().apply {
                put(KeyExchange.PUBLIC_KEY, nextStep.publicKey)
                put(KeyExchange.TYPE, nextStep.type)
            }.toString()

            Logger.log("Sending key exchange message $exchangeMessage")
            sendKeyExchangeMesage(exchangeMessage)
        }
    }

    fun sendMessage(message: String) {
        Logger.log("CommunicationClient:: Sending message: $message")
        val message = Bundle().apply {
            putString(MESSAGE, message)
        }

        if (keyExchange.keysExchanged()) {
            messageService?.sendMessage(message)
        } else {
            addRequestJob { messageService?.sendMessage(message) }
        }
    }

    fun sendRequest(request: EthereumRequest, callback: (Any?) -> Unit) {
        if (!isServiceConnected) {
            Logger.log("CommunicationClient:: sendRequest - no longer connected to metamask, binding service first")
            addRequestJob { processRequest(request, callback) }
            bindService()
        } else if (!keyExchange.keysExchanged()) {
            Logger.log("CommunicationClient:: sendRequest - keys not yet exchanged")
            addRequestJob { processRequest(request, callback) }
            initiateKeyExchange()
        } else {
            processRequest(request, callback)
        }
    }

    private fun processRequest(request: EthereumRequest, callback: (Any?) -> Unit) {
        val requestJson = Gson().toJson(request)
        Logger.log("CommunicationClient:: sending request $requestJson")

        val payload = keyExchange.encrypt(requestJson)
        val message = Message(sessionId, payload)
        val messageJson = Gson().toJson(message)

        submittedRequests[request.id] = SubmittedRequest(request, callback)
        sendMessage(messageJson)
    }

    private fun sendOriginatorInfo() {
        val originatorInfo = OriginatorInfo(dapp?.name, dapp?.url, SDKInfo.PLATFORM, SDKInfo.VERSION)
        val requestInfo = RequestInfo("originator_info", originatorInfo)
        val requestInfoJson = Gson().toJson(requestInfo)

        Logger.log("CommunicationClient:: Sending originator info: $requestInfoJson")

        val payload = keyExchange.encrypt(requestInfoJson)

        val message = Message(sessionId, payload)
        val messageJson = Gson().toJson(message)

        sendMessage(messageJson)
    }

    fun bindService() {
        Logger.log("CommunicationClient:: Binding service")

        val serviceIntent = Intent()
            .setComponent(
                ComponentName(
                    "io.metamask",
                    "io.metamask.nativesdk.MessageService"
                )
            )

        if (appContextRef.get() != null) {
            appContextRef.get()?.bindService(
                serviceIntent,
                serviceConnection,
                Context.BIND_AUTO_CREATE)
        } else {
            Logger.error("App context null!")
        }
    }

    fun unbindService() {
        if (isServiceConnected) {
            Logger.log("CommunicationClient:: Unbinding service")
            appContextRef.get()?.unbindService(serviceConnection)
            isServiceConnected = false
        }
    }

    fun initiateKeyExchange() {
        Logger.log("CommunicationClient:: Initiating key exchange")

        val keyExchange = JSONObject().apply {
            put(KeyExchange.PUBLIC_KEY, keyExchange.publicKey)
            put(KeyExchange.TYPE, KeyExchangeMessageType.KEY_HANDSHAKE_SYN.name)
        }

        sendKeyExchangeMesage(keyExchange.toString())
    }

    private fun sendKeyExchangeMesage(message: String) {
        Logger.log("Sending key exchange $message")
        val message = Bundle().apply {
            putString(KEY_EXCHANGE, message)
        }
        messageService?.sendMessage(message)
    }
}