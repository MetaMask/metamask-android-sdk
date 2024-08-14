package io.metamask.androidsdk

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.serialization.Serializable
import org.json.JSONObject
import java.lang.ref.WeakReference

class CommunicationClient(
    context: Context,
    callback: EthereumEventCallback?,
    private val sessionManager: SessionManager,
    private val keyExchange: KeyExchange,
    private val serviceConnection: ClientServiceConnection,
    private val messageServiceCallback: ClientMessageServiceCallback,
    private val tracker: Tracker,
    private val logger: Logger = DefaultLogger)  {

    var sessionId: String = ""

    var dappMetadata: DappMetadata? = null
    var isServiceConnected = false
        private set

    private val appContextRef: WeakReference<Context> = WeakReference(context)
    var ethereumEventCallbackRef: WeakReference<EthereumEventCallback> = WeakReference(callback)

    var requestJobs: MutableList<() -> Unit> = mutableListOf()
        private set

    var submittedRequests: MutableMap<String, SubmittedRequest>  = mutableMapOf()
        private set

    var queuedRequests: MutableMap<String, SubmittedRequest>  = mutableMapOf()
        private set

    private var isMetaMaskReady = false
    private var sentOriginatorInfo = false

    var requestedBindService = false
        private set

    var enableDebug: Boolean = false
        set(value) {
        field = value
        tracker.enableDebug = value
    }

    init {
        sessionId = sessionManager.sessionId
        // in case not yet initialised in SessionManager
        sessionManager.onInitialized = {
            sessionId = sessionManager.sessionId
        }
        setupServiceConnection()
        setupMessageServiceCallback()
    }

    fun resetState() {
        sentOriginatorInfo = false
        submittedRequests.clear()
        queuedRequests.clear()
        requestJobs.clear()
    }

    private fun setupServiceConnection() {
        serviceConnection.onConnected = {
            logger.log("CommunicationClient:: Service connected")
            isServiceConnected = true
            serviceConnection.registerCallback(messageServiceCallback)
            initiateKeyExchange()
        }

        serviceConnection.onDisconnected = { name ->
            isServiceConnected = false
            logger.error("CommunicationClient:: Service disconnected $name")
            trackEvent(Event.SDK_DISCONNECTED)
        }

        serviceConnection.onBindingDied = { name ->
            logger.error("CommunicationClient:: binding died: $name")
        }

        serviceConnection.onNullBinding = { name ->
            logger.error("CommunicationClient:: null binding: $name")
        }
    }

    private fun setupMessageServiceCallback() {
        messageServiceCallback.onMessage = { bundle ->
            val keyExchange = bundle.getString(KEY_EXCHANGE)
            val message = bundle.getString(MESSAGE)

            if (keyExchange != null) {
                handleKeyExchange(keyExchange)
            } else if (message != null) {
                handleMessage(message)
            }
        }
    }

    fun trackEvent(event: Event, params: Map<String, String> = mapOf()) {
        val parameters: MutableMap<String, String> = mutableMapOf(
            "id" to sessionId
        )
        parameters.putAll(params)

        when(event) {
            Event.SDK_CONNECTION_REQUEST_STARTED -> {
                parameters["commLayer"] = SDKInfo.PLATFORM
                parameters["sdkVersion"] = SDKInfo.VERSION
                parameters["url"] = dappMetadata?.url ?: ""
                parameters["title"] = dappMetadata?.name ?: ""
                parameters["platform"] = SDKInfo.PLATFORM
                parameters["channelId"] = sessionId
            }
            Event.SDK_RPC_REQUEST -> {
                parameters["commLayer"] = SDKInfo.PLATFORM
                parameters["sdkVersion"] = SDKInfo.VERSION
                parameters["url"] = dappMetadata?.url ?: ""
                parameters["title"] = dappMetadata?.name ?: ""
                parameters["platform"] = SDKInfo.PLATFORM
                parameters["timestamp"] = TimeStampGenerator.timestamp()
                parameters["channelId"] = sessionId
                parameters["from"] = "mobile"
            }
            else -> Unit
        }
        tracker.trackEvent(event, parameters)
    }

    fun updateSessionDuration(duration: Long) {
        sessionManager.updateSessionDuration(duration)
    }

    fun clearSession(onComplete: () -> Unit) {
        sessionManager.clearSession {
            sessionId = sessionManager.sessionId
            keyExchange.reset()
            onComplete()
        }
        sentOriginatorInfo = false
    }

    fun handleMessage(message: String) {
        val jsonString = keyExchange.decrypt(message)
        val json = JSONObject(jsonString)

        when (json.optString(MessageType.TYPE.value)) {
            MessageType.TERMINATE.value -> {
                logger.log("CommunicationClient:: Connection terminated by MetaMask")
                unbindService()
                keyExchange.reset()
            }
            MessageType.KEYS_EXCHANGED.value -> {
                logger.log("CommunicationClient:: Keys exchanged")
                keyExchange.complete()
                sendOriginatorInfo()
            }
            MessageType.READY.value -> {
                logger.log("CommunicationClient:: Connection ready")
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
                    logger.log("CommunicationClient:: Received error $json")
                    val id = json.optString("id")
                    val error = json.optString(MessageType.ERROR.value)
                    handleError(error, id)
                }
            }
        }
    }

    fun resumeRequestJobs() {
        logger.log("CommunicationClient:: Resuming jobs")

        while (requestJobs.isNotEmpty()) {
            val job = requestJobs.removeFirstOrNull()
            job?.invoke()
        }
    }

    fun queueRequestJob(job: () -> Unit) {
        requestJobs.add(job)
        logger.log("CommunicationClient:: Queued job")
    }

    fun clearPendingRequests() {
        queuedRequests = mutableMapOf()
        requestJobs = mutableListOf()
        submittedRequests = mutableMapOf()
    }

    fun handleResponse(id: String, data: JSONObject) {
        val submittedRequest = submittedRequests[id]?.request ?: return

        val error = data.optString("error")

        val params = mapOf(
            "method" to submittedRequest.method,
            "from" to "mobile"
        )
        trackEvent(Event.SDK_RPC_REQUEST_DONE, params)

        if (handleError(error, id)) {
            return
        }

        val isResultMethod = EthereumMethod.isResultMethod(submittedRequest.method)

        if (!isResultMethod) {
            val resultJson = data.optString("result")

            if (resultJson.isNotEmpty()) {
                val result: Map<String, Any?>? = Gson().fromJson(resultJson, object : TypeToken<Map<String, Any?>>() {}.type)
                if (result != null) {
                    submittedRequests[id]?.callback?.invoke(Result.Success.ItemMap(result))
                    completeRequest(id, Result.Success.ItemMap(result))
                } else {
                    val accounts: List<String>? = Gson().fromJson(resultJson, object : TypeToken<List<String>>() {}.type)
                    val account = accounts?.firstOrNull()
                    if (account != null) {
                        submittedRequests[id]?.callback?.invoke(Result.Success.Item(account))
                        completeRequest(id, Result.Success.Item(account))
                    }
                }
            } else {
                val result: Map<String, Serializable> = Gson().fromJson(data.toString(), object : TypeToken<Map<String, Serializable>>() {}.type)
                completeRequest(id, Result.Success.ItemMap(result))
            }
            return
        }

        when(submittedRequest.method) {
            EthereumMethod.GET_METAMASK_PROVIDER_STATE.value -> {
                val result = data.optString("result")
                val resultJson = JSONObject(result)
                val accountsJson = resultJson.optString("accounts")
                val accounts: List<String> = Gson().fromJson(accountsJson, object : TypeToken<List<String>>() {}.type)

                val account = accounts.firstOrNull()

                if (account != null) {
                    updateAccount(account)
                    completeRequest(id, Result.Success.Item(account))
                }

                val chainId = resultJson.optString("chainId")

                if (chainId.isNotEmpty()) {
                    updateChainId(chainId)
                    completeRequest(id, Result.Success.Item(chainId))
                }
            }
            EthereumMethod.ETH_REQUEST_ACCOUNTS.value -> {
                val result = data.optString("result")
                val accounts: List<String> = Gson().fromJson(result, object : TypeToken<List<String>>() {}.type)
                val selectedAccount = accounts.getOrNull(0)

                if (selectedAccount != null) {
                    updateAccount(selectedAccount)
                }

                completeRequest(id, Result.Success.Items(accounts))
            }
            EthereumMethod.ETH_CHAIN_ID.value -> {
                val chainId = data.optString("result")

                if (chainId.isNotEmpty()) {
                    updateChainId(chainId)
                    completeRequest(id, Result.Success.Item(chainId))
                }
            }
            EthereumMethod.ETH_SIGN_TYPED_DATA_V3.value,
            EthereumMethod.ETH_SIGN_TYPED_DATA_V4.value,
            EthereumMethod.ETH_SEND_TRANSACTION.value -> {
                val result = data.optString("result")

                if (result.isNotEmpty()) {
                    completeRequest(id, Result.Success.Item(result))
                } else {
                    logger.error("CommunicationClient:: Unexpected response: $data")
                }
            }
            EthereumMethod.METAMASK_BATCH.value -> {
                val result = data.optString("result")
                val results: List<String?> = Gson().fromJson(result, object : TypeToken<List<String?>>() {}.type)
                val sanitisedResults = results.filterNotNull()
                completeRequest(id, Result.Success.Items(sanitisedResults))
            }
            else -> {
                val result = data.optString("result")
                completeRequest(id, Result.Success.Item(result))
            }
        }
    }

    fun handleError(error: String, id: String): Boolean {
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
        logger.error("CommunicationClient:: Got error $message")
        completeRequest(requestId, Result.Error(RequestError(code, message)))
        return true
    }

    fun completeRequest(id: String, result: Result) {
        if (queuedRequests[id] != null) {
            queuedRequests[id]?.callback?.invoke(result)
            queuedRequests.remove(id)
        }
        submittedRequests[id]?.callback?.invoke(result)
        submittedRequests.remove(id)
    }

    fun handleEvent(event: JSONObject) {
        when (event.optString("method")) {
            EthereumMethod.METAMASK_ACCOUNTS_CHANGED.value -> {
                val accountsJson = event.optString("params")
                val accounts: List<String> = Gson().fromJson(accountsJson, object : TypeToken<List<String>>() {}.type)
                accounts.getOrNull(0)?.let { account ->
                    logger.log("CommunicationClient:: Event Updated to account $account")
                    updateAccount(account)
                }
            }
            EthereumMethod.METAMASK_CHAIN_CHANGED.value -> {
                val paramsJson = event.optJSONObject("params")
                val chainId = paramsJson?.optString("chainId")

                if (!chainId.isNullOrEmpty()) {
                    updateChainId(chainId)
                }
            }
            else -> {
                logger.error("CommunicationClient:: Unexpected event: $event")
            }
        }
    }

    fun updateAccount(account: String) {
        val callback = ethereumEventCallbackRef.get()
        callback?.updateAccount(account)
    }

    fun updateChainId(chainId: String) {
        val callback = ethereumEventCallbackRef.get()
        callback?.updateChainId(chainId)
    }

    fun handleKeyExchange(message: String) {
        val json = JSONObject(message)

        val keyExchangeStep = json.optString(KeyExchange.TYPE, KeyExchangeMessageType.KEY_HANDSHAKE_SYN.name)
        val type = KeyExchangeMessageType.valueOf(keyExchangeStep)
        val theirPublicKey = json.optString(KeyExchange.PUBLIC_KEY)
        val keyExchangeMessage = KeyExchangeMessage(type.name, theirPublicKey)
        val nextStep  = keyExchange.nextKeyExchangeMessage(keyExchangeMessage)

        if (type == KeyExchangeMessageType.KEY_HANDSHAKE_ACK) {
            keyExchange.complete()
        }

        if (nextStep != null) {
            val exchangeMessage = JSONObject().apply {
                put(KeyExchange.PUBLIC_KEY, nextStep.publicKey)
                put(KeyExchange.TYPE, nextStep.type)
            }.toString()

            logger.log("Sending key exchange ${nextStep.type}")
            sendKeyExchangeMesage(exchangeMessage)
        }
    }

    fun sendMessage(message: String) {
        val bundle = Bundle().apply {
            putString(MESSAGE, message)
        }

        if (keyExchange.keysExchanged()) {
            serviceConnection.sendMessage(bundle)
        } else {
            logger.log("CommunicationClient::sendMessage keys not exchanged, queueing job")
            queueRequestJob { serviceConnection.sendMessage(bundle) }
        }
    }

    fun sendRequest(request: RpcRequest, callback: (Result) -> Unit) {
        if (request.method == EthereumMethod.GET_METAMASK_PROVIDER_STATE.value) {
            clearPendingRequests()
        }

        if (!isServiceConnected) {
            queuedRequests[request.id] = SubmittedRequest(request, callback)
            queueRequestJob { processRequest(request, callback) }
            if (!requestedBindService) {
                logger.log("CommunicationClient:: sendRequest - not yet connected to metamask, binding service first")
                bindService()
            } else {
                logger.log("CommunicationClient:: sendRequest - not yet connected to metamask, waiting for service to bind")
            }
        } else if (!keyExchange.keysExchanged()) {
            logger.log("CommunicationClient:: sendRequest - keys not yet exchanged")
            queuedRequests[request.id] = SubmittedRequest(request, callback)
            queueRequestJob { processRequest(request, callback) }
            initiateKeyExchange()
        } else {
            if (isMetaMaskReady) {
                processRequest(request, callback)
            } else {
                logger.log("CommunicationClient::sendRequest - wallet is not ready, queueing request")
                queueRequestJob { processRequest(request, callback) }
                sendOriginatorInfo()
            }
        }
    }

    fun processRequest(request: RpcRequest, callback: (Result) -> Unit) {
        logger.log("CommunicationClient:: sending request $request")
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

    fun sendOriginatorInfo() {
        if (sentOriginatorInfo) { return }
        sentOriginatorInfo = true

        val originatorInfo = OriginatorInfo(
            title = dappMetadata?.name,
            url = dappMetadata?.url,
            icon = dappMetadata?.iconUrl ?: dappMetadata?.base64Icon,
            dappId = appContextRef.get()?.packageName,
            platform = SDKInfo.PLATFORM,
            apiVersion = SDKInfo.VERSION)
        val requestInfo = RequestInfo("originator_info", originatorInfo)
        val requestInfoJson = Gson().toJson(requestInfo)

        logger.log("CommunicationClient:: Sending originator info: $requestInfoJson")

        val payload = keyExchange.encrypt(requestInfoJson)

        val message = Message(sessionId, payload)
        val messageJson = Gson().toJson(message)

        sendMessage(messageJson)
    }

    fun isQA(): Boolean {
        if (Build.VERSION.SDK_INT < 33 ) { // i.e Build.VERSION_CODES.TIRAMISU
            return false
        }

        val packageManager = appContextRef.get()?.packageManager

        return try {
            packageManager?.getPackageInfo("io.metamask.qa", PackageManager.PackageInfoFlags.of(0))
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun bindService() {
        logger.log("CommunicationClient:: Binding service")
        requestedBindService = true
        
        val serviceIntent = Intent()
            .setComponent(
                ComponentName(
                    if (isQA()) "io.metamask.qa" else "io.metamask",
                    "io.metamask.nativesdk.MessageService"
                )
            )

        if (appContextRef.get() != null) {
            appContextRef.get()?.bindService(
                serviceIntent,
                serviceConnection,
                Context.BIND_AUTO_CREATE)
        } else {
            logger.error("App context null")
        }
    }

    fun unbindService() {
        requestedBindService = false

        if (isServiceConnected) {
            logger.log("CommunicationClient:: unbindService")
            appContextRef.get()?.unbindService(serviceConnection)
            isServiceConnected = false
        }
    }

    fun initiateKeyExchange() {
        logger.log("CommunicationClient:: Initiating key exchange")

        val keyExchange = JSONObject().apply {
            put(KeyExchange.PUBLIC_KEY, keyExchange.publicKey)
            put(KeyExchange.TYPE, KeyExchangeMessageType.KEY_HANDSHAKE_SYN.name)
        }

        logger.log("Sending key exchange ${KeyExchangeMessageType.KEY_HANDSHAKE_SYN}")
        sendKeyExchangeMesage(keyExchange.toString())
    }

    fun sendKeyExchangeMesage(message: String) {
        val bundle = Bundle().apply {
            putString(KEY_EXCHANGE, message)
        }
        serviceConnection.sendMessage(bundle)
    }
}