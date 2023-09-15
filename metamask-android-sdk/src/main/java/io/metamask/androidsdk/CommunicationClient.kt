package io.metamask.androidsdk

import android.os.Bundle
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.serialization.Serializable
import org.json.JSONObject
import java.lang.ref.WeakReference
import javax.inject.Provider

class CommunicationClient(
    private val tracker: Tracker,
    private val keyExchange: KeyExchange,
    callback: Provider<EthereumEventCallback>,
    private val sessionManager: SessionManager,
    private val remoteServiceConnection: RemoteServiceConnection
): RemoteMessageServiceCallback  {

    var sessionId: String

    var dapp: Dapp? = null
    private val ethereumEventCallback: EthereumEventCallback by lazy {
        callback.get()
    }
    //private val ethereumEventCallbackRef: WeakReference<EthereumEventCallback> = WeakReference(callback)

    private var requestJobs: MutableList<() -> Unit> = mutableListOf()
    private var submittedRequests: MutableMap<String, SubmittedRequest>  = mutableMapOf()
    private var queuedRequests: MutableMap<String, SubmittedRequest>  = mutableMapOf()

    private var isMetaMaskReady = false
    private var sentOriginatorInfo = false

    var enableDebug: Boolean = false
        set(value) {
        field = value
        tracker.enableDebug = value
    }

    init {
        sessionId = sessionManager.sessionId
    }

    fun trackEvent(event: Event, params: MutableMap<String, String>?) {
        val parameters: MutableMap<String, String> = params ?: mutableMapOf(
            "id" to sessionId
        )

        when(event) {
            Event.SDK_CONNECTION_REQUEST_STARTED -> {
                parameters["commlayer"] = SDKInfo.PLATFORM
                parameters["sdkVersion"] = SDKInfo.VERSION
                parameters["url"] = dapp?.url ?: ""
                parameters["title"] = dapp?.name ?: ""
                parameters["platform"] = SDKInfo.PLATFORM
            }
            else -> Unit
        }
        tracker.trackEvent(event, parameters)
    }

    fun setSessionDuration(duration: Long) {
        sessionManager.sessionDuration = duration
    }

    fun clearSession() {
        sessionManager.clearSession()
        sessionId = sessionManager.sessionId
        keyExchange.reset()
    }

    private fun handleMessage(message: String) {
        val jsonString = keyExchange.decrypt(message)
        Logger.log("CommunicationClient:: Got message: $jsonString")
        val json = JSONObject(jsonString)

        when (json.optString(MessageType.TYPE.value)) {
            MessageType.TERMINATE.value -> {
                Logger.log("CommunicationClient:: Connection terminated by MetaMask")
                remoteServiceConnection.disconnect()
                keyExchange.reset()
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
            else -> {
                val data = json.optString(MessageType.DATA.value)

                if (data.isNotEmpty()) {
                    val dataJson = JSONObject(data)
                    val id = dataJson.optString(MessageType.ID.value)

                    if (id.isNotEmpty()) {
                        handleResponse(id, dataJson)
                    } else if (dataJson.optString(MessageType.ERROR.value).isNotEmpty()) {
                        handleError(dataJson.optString(MessageType.ERROR.value), "")
                        sentOriginatorInfo = false // connection request rejected
                    } else {
                        handleEvent(dataJson)
                    }
                } else {
                    val id = json.optString("id")
                    val error = json.optString(MessageType.ERROR.value)
                    handleError(error, id)
                }
            }
        }
    }

    private fun resumeRequestJobs() {
        Logger.log("CommunicationClient:: Resuming jobs")

        while (requestJobs.isNotEmpty()) {
            val job = requestJobs.removeFirstOrNull()
            job?.invoke()
        }
    }

    private fun queueRequestJob(job: () -> Unit) {
        requestJobs.add(job)
        Logger.log("CommunicationClient:: Queued job")
    }

    private fun clearPendingRequests() {
        queuedRequests = mutableMapOf()
        requestJobs = mutableListOf()
        submittedRequests = mutableMapOf()
    }

    private fun handleResponse(id: String, data: JSONObject) {
        val error = data.optString("error")

        if (handleError(error, id)) {
            return
        }

        val request = submittedRequests[id]?.request
        Logger.log("CommunicationClient:: Response for request ${request?.method}")
        val isResultMethod = EthereumMethod.isResultMethod(request?.method ?: "")

        if (!isResultMethod) {
            val resultJson = data.optString("result")

            if (resultJson.isNotEmpty()) {
                var result: Map<String, Any?>? = Gson().fromJson(resultJson, object : TypeToken<Map<String, Any?>>() {}.type)
                if (result != null) {
                    submittedRequests[id]?.callback?.invoke(result)
                    completeRequest(id, result)
                } else {
                    val accounts: List<String>? = Gson().fromJson(resultJson, object : TypeToken<List<String>>() {}.type)
                    Logger.log("CommunicationClient:: Accounts: $accounts")
                    val account = accounts?.firstOrNull()
                    if (account != null) {
                        submittedRequests[id]?.callback?.invoke(account)
                        completeRequest(id, account)
                    }
                }

            } else {
                val result: Map<String, Serializable> = Gson().fromJson(data.toString(), object : TypeToken<Map<String, Serializable>>() {}.type)
                completeRequest(id, result)
            }
            return
        }

        when(request?.method) {
            EthereumMethod.GET_METAMASK_PROVIDER_STATE.value -> {
                val result = data.optString("result")
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
            EthereumMethod.ETH_REQUEST_ACCOUNTS.value  -> {
                val result = data.optString("result")
                val accounts: List<String> = Gson().fromJson(result, object : TypeToken<List<String>>() {}.type)
                val account = accounts.getOrNull(0)

                if (account != null) {
                    updateAccount(account)
                    completeRequest(id, account)
                } else {
                    Logger.error("CommunicationClient:: Request accounts failure: $result")
                }
            }
            EthereumMethod.ETH_CHAIN_ID.value -> {
                val chainId = data.optString("result")

                if (chainId.isNotEmpty()) {
                    updateChainId(chainId)
                    completeRequest(id, chainId)
                }
            }
            EthereumMethod.ETH_SIGN_TYPED_DATA_V3.value,
            EthereumMethod.ETH_SIGN_TYPED_DATA_V4.value,
            EthereumMethod.ETH_SEND_TRANSACTION.value -> {
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

        val requestId: String = id.ifEmpty {
            queuedRequests.entries.find { it.value.request.method == EthereumMethod.ETH_REQUEST_ACCOUNTS.value }?.key ?: ""
        }

        val errorMap: Map<String, Any?> = Gson().fromJson(error, object : TypeToken<Map<String, Any?>>() {}.type)
        val errorCode = errorMap["code"] as? Double ?: -1
        val code = errorCode.toInt()
        val message = errorMap["message"] as? String ?: ErrorType.message(code)
        completeRequest(requestId, RequestError(code, message))
        return true
    }

    private fun completeRequest(id: String, result: Any) {
        if (queuedRequests[id] != null) {
            queuedRequests[id]?.callback?.invoke(result)
            queuedRequests.remove(id)
        }
        submittedRequests[id]?.callback?.invoke(result)
        submittedRequests.remove(id)
    }

    private fun handleEvent(event: JSONObject) {
        when (event.optString("method")) {
            EthereumMethod.METAMASK_ACCOUNTS_CHANGED.value -> {
                val accountsJson = event.optString("params")
                val accounts: List<String> = Gson().fromJson(accountsJson, object : TypeToken<List<String>>() {}.type)

                accounts.getOrNull(0)?.let { account ->
                    updateAccount(account)
                }
            }
            EthereumMethod.METAMASK_CHAIN_CHANGED.value -> {
                val paramsJson = event.optJSONObject("params")
                val chainId = paramsJson?.optString("chainId")

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
        Logger.log("CommunicationClient:: Received account changed event: $account")
        ethereumEventCallback.updateAccount(account)
    }

    private fun updateChainId(chainId: String) {
        Logger.log("CommunicationClient:: Received chain changed event: $chainId")
        ethereumEventCallback.updateChainId(chainId)
    }

    override fun handleRemoteMessage(bundle: Bundle) {
        val keyExchange = bundle.getString(KEY_EXCHANGE)
        val message = bundle.getString(MESSAGE)

        if (keyExchange != null) {
            handleKeyExchange(keyExchange)
        } else if (message != null) {
            handleMessage(message)
        }
    }

    private fun handleKeyExchange(message: String) {
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

    private fun sendMessage(message: String) {
        val bundle = Bundle().apply {
            putString(MESSAGE, message)
        }

        if (keyExchange.keysExchanged()) {
            remoteServiceConnection.sendMessage(bundle)
        } else {
            Logger.log("CommunicationClient::sendMessage keys not exchanged, queueing job")
            queueRequestJob { remoteServiceConnection.sendMessage(bundle) }
        }
    }

    fun sendRequest(request: EthereumRequest, callback: (Any?) -> Unit) {
        if (request.method == EthereumMethod.GET_METAMASK_PROVIDER_STATE.value) {
            clearPendingRequests()
        }

        if (!remoteServiceConnection.isServiceConnected) {
            Logger.log("CommunicationClient:: sendRequest - not yet connected to metamask, binding service first")
            queuedRequests[request.id] = SubmittedRequest(request, callback)
            queueRequestJob { processRequest(request, callback) }
            remoteServiceConnection.connect()
        } else if (!keyExchange.keysExchanged()) {
            Logger.log("CommunicationClient:: sendRequest - keys not yet exchanged")
            queuedRequests[request.id] = SubmittedRequest(request, callback)
            queueRequestJob { processRequest(request, callback) }
            initiateKeyHandshake()
        } else {
            if (isMetaMaskReady) {
                processRequest(request, callback)
            } else {
                Logger.log("CommunicationClient::sendRequest - MetaMask is not ready, queueing request")
                queueRequestJob { processRequest(request, callback) }
                sendOriginatorInfo()
            }
        }
    }

    private fun processRequest(request: EthereumRequest, callback: (Any?) -> Unit) {
        Logger.log("CommunicationClient:: sending request $request")
        if (queuedRequests[request.id] != null) {
            queuedRequests.remove(request.id)
        }

        val requestJson = Gson().toJson(request)

        val payload = keyExchange.encrypt(requestJson)
        val message = Message(sessionId, payload)
        val messageJson = Gson().toJson(message)

        submittedRequests[request.id] = SubmittedRequest(request, callback)
        sendMessage(messageJson)
    }

    private fun sendOriginatorInfo() {
        if (sentOriginatorInfo) { return }
        sentOriginatorInfo = true

        val originatorInfo = OriginatorInfo(dapp?.name, dapp?.url, SDKInfo.PLATFORM, SDKInfo.VERSION)
        val requestInfo = RequestInfo("originator_info", originatorInfo)
        val requestInfoJson = Gson().toJson(requestInfo)

        Logger.log("CommunicationClient:: Sending originator info: $requestInfoJson")

        val payload = keyExchange.encrypt(requestInfoJson)

        val message = Message(sessionId, payload)
        val messageJson = Gson().toJson(message)

        sendMessage(messageJson)
    }

    override fun initiateKeyHandshake() {
        Logger.log("CommunicationClient:: Initiating key exchange")

        val keyExchange = JSONObject().apply {
            put(KeyExchange.PUBLIC_KEY, keyExchange.publicKey)
            put(KeyExchange.TYPE, KeyExchangeMessageType.KEY_HANDSHAKE_SYN.name)
        }

        sendKeyExchangeMesage(keyExchange.toString())
    }

    private fun sendKeyExchangeMesage(message: String) {
        Logger.log("Sending key exchange $message")
        val bundle = Bundle().apply {
            putString(KEY_EXCHANGE, message)
        }
        remoteServiceConnection.sendMessage(bundle)
    }
}