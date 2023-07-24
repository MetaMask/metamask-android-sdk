package io.metamask.androidsdk

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.metamask.nativesdk.IMessegeService
import io.metamask.nativesdk.IMessegeServiceCallback
import org.json.JSONObject
import kotlinx.serialization.Serializable
import java.lang.ref.WeakReference

class CommunicationClient(context: Context, lifecycle: Lifecycle, callback: EthereumEventCallback)  {
    private val appContext = context.applicationContext

    val sessionId: String
    private val keyExchange: KeyExchange

    var dapp: Dapp? = null
    var isServiceConnected = false
        private set

    private val tracker: Tracker = Analytics()

    private var messageService: IMessegeService? = null
    private val ethereumEventCallbackRef: WeakReference<EthereumEventCallback> = WeakReference(callback)

    private var requestJobs: MutableList<() -> Unit> = mutableListOf()
    private var submittedRequests: MutableMap<String, SubmittedRequest>  = mutableMapOf()

    private val observer = object : DefaultLifecycleObserver {

        override fun onCreate(owner: LifecycleOwner) {
            Logger.log("CommClient: onCreate()")
        }

        override fun onStart(owner: LifecycleOwner) {
            Logger.log("CommClient: onStart()")
        }

        override fun onDestroy(owner: LifecycleOwner) {
            Logger.log("CommClient: onDestroy: Disconnecting...")
            unbindService()
        }
    }

    private var sessionManager: SessionManager

    init {
        lifecycle.addObserver(observer)
        keyExchange = KeyExchange()
        sessionManager = SessionManager(KeyStorage(context))
        sessionId = sessionManager.sessionId
        Logger.log("CommClient: sessionId: $sessionId")
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            messageService = IMessegeService.Stub.asInterface(service)
            messageService?.registerCallback(messageServiceCallback)
            isServiceConnected = true
            Log.d(TAG,"CommClient: Service connected $name")
            initiateKeyExchange()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            messageService = null
            isServiceConnected = false
            Log.e(TAG,"CommClient: Service disconnected $name")
            trackEvent(Event.DISCONNECTED, null)
        }

        override fun onBindingDied(name: ComponentName?) {
            Logger.log("CommClient: binding died: $name")
        }

        override fun onNullBinding(name: ComponentName?) {
            Logger.log("CommClient: null binding: $name")
        }
    }

    private val messageServiceCallback: IMessegeServiceCallback = object : IMessegeServiceCallback.Stub() {
        override fun onMessageReceived(message: Bundle) {
            Logger.log("CommClient: onMessageReceived!")

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
                params["commlayer"] = "android"
                params["sdkVersion"] = "0.2.0"
                params["url"] = dapp?.url ?: ""
                params["title"] = dapp?.name ?: ""
                params["platform"] = "android"
            }
            else -> Unit
        }

        tracker.trackEvent(event, params)
    }

    fun setSessionDuration(duration: Long) {
        sessionManager.setSessionDuration(duration)
    }

    private fun handleMessage(message: String) {
        val jsonString = keyExchange.decrypt(message)
        Logger.log("CommClient: Received message: $jsonString")
        val json = JSONObject(jsonString)

        when (json.optString(MessageType.TYPE.value)) {
            MessageType.TERMINATE.value -> {
                Logger.log("Connection terminated")
                unbindService()
                keyExchange.generateNewKeys()
                bindService()
            }
            MessageType.PAUSE.value -> {
                Logger.log("Connection paused")
            }
            MessageType.KEYS_EXCHANGED.value -> {
                Logger.log("Keys exchanged")
                sendOriginatorInfo()
            }
            MessageType.READY.value -> {
                Logger.log("Connection ready")
                resumeRequestJobs()
            }
            MessageType.WALLET_INFO.value -> {
                Logger.log("Received wallet info $json")
            }
            else -> {
                val data = json.optString(MessageType.DATA.value)

                Logger.log("Received some data $data")

                if (data.isNotEmpty()) {
                    val dataJson = JSONObject(data)
                    val id = dataJson.optString(MessageType.ID.value)

                    if (id.isNotEmpty()) {
                        handleResponse(id, dataJson)
                    } else if (dataJson.optString(MessageType.ERROR.value).isNotEmpty()) {
                        Logger.error("Got error $dataJson")
                    } else {
                        handleEvent(dataJson)
                    }
                }
            }
        }
    }

    private fun resumeRequestJobs() {
        Logger.log("Resuming jobs")

        while (requestJobs.isNotEmpty()) {
            Logger.log("Running queued job")
            val job = requestJobs.removeFirstOrNull()
            job?.invoke()
        }
    }

    private fun addRequestJob(job: () -> Unit) {
        Logger.log("Queued job")
        requestJobs.add(job)
    }

    private fun handleResponse(id: String, data: JSONObject) {
        // check for error
        Logger.log("Got response: ${data}")
        val error = data.optString("error")

        if (handleError(error, id)) {
            return
        }

        val request = submittedRequests[id]?.request
        val isResultMethod = EthereumMethod.isResultMethod(request?.method ?: "")

        if (!isResultMethod) {
            val resultJson = data.optString("result")

            if (resultJson.isNotEmpty()) {
                val result: Map<String, Any?> = Gson().fromJson(resultJson, object : TypeToken<Map<String, Any?>>() {}.type)
                submittedRequests[id]?.callback?.invoke(result)
                completeRequest(id, result)
            } else {
                val result: Map<String, Serializable> = Gson().fromJson(data.toString(), object : TypeToken<Map<String, Serializable>>() {}.type)
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
                Logger.log("eth_requestAccounts response: $result")
                val accounts: List<String> = Gson().fromJson(result, object : TypeToken<List<String>>() {}.type)
                val account = accounts.getOrNull(0)

                if (account != null) {
                    updateAccount(account)
                    completeRequest(id, account)
                } else {
                    Logger.error("Request accounts failure")
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
                    Logger.error("Unexpected response: $data")
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
        Logger.log("Got event: $event")

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

                Logger.log("Got metamask_chainChanged: params: $paramsJson, chainId: $chainId")

                if (chainId != null && chainId.isNotEmpty()) {
                    updateChainId(chainId)
                }
            }
            else -> {
                Logger.error("Unexpected event: $event")
            }
        }
    }

    private fun updateAccount(account: String) {
        Logger.log("CommClient: Received account event: $account")
        val callback = ethereumEventCallbackRef.get()
        callback?.updateAccount(account)
    }

    private fun updateChainId(chainId: String) {
        Logger.log("CommClient: Received chain event: $chainId")
        val callback = ethereumEventCallbackRef.get()
        callback?.updateChainId(chainId)
    }

    private fun handleKeyExchange(message: String) {
        Logger.log("CommClient: Received key exchange $message")

        val json = JSONObject(message)

        val keyExchangeStep = json.optString(KeyExchange.TYPE, KeyExchangeMessageType.key_exchange_SYN.name)
        val type = KeyExchangeMessageType.valueOf(keyExchangeStep)
        val theirPublicKey = json.optString(KeyExchange.PUBLIC_KEY)
        val keyExchangeMessage = KeyExchangeMessage(type.name, theirPublicKey)
        val nextStep  = keyExchange.nextKeyExchangeMessage(keyExchangeMessage)

        if (
            type == KeyExchangeMessageType.key_exchange_SYNACK ||
            type == KeyExchangeMessageType.key_exchange_ACK) {
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
        Logger.log("CommClient: Sending message: $message")
        val message = Bundle().apply {
            putString(MESSAGE, message)
        }

        if (!keyExchange.keysExchanged) {
            addRequestJob { messageService?.sendMessage(message) }
        } else {
            messageService?.sendMessage(message)
        }
    }

    fun sendRequest(request: EthereumRequest, callback: (Any?) -> Unit) {
        if (keyExchange.keysExchanged) {
            if (!isServiceConnected) {
                Logger.log("CommClient: no longer connected to metamask, binding service first")
                bindService()
                addRequestJob { processRequest(request, callback) }
            } else {
                processRequest(request, callback)
            }
        } else {
            Logger.log("CommClient: sendRequest - keys not yet exchange")
            addRequestJob { processRequest(request, callback) }
        }
    }

    private fun processRequest(request: EthereumRequest, callback: (Any?) -> Unit) {
        val requestJson = Gson().toJson(request)
        Logger.log("CommClient: sending request $requestJson")

        val payload = keyExchange.encrypt(requestJson)
        val message = Message(sessionId, payload)
        val messageJson = Gson().toJson(message)

        submittedRequests[request.id] = SubmittedRequest(request, callback)
        sendMessage(messageJson)
    }

    private fun sendOriginatorInfo() {
        val manager = appContext.packageManager
        // (TODO) this incorrectly gets the app package info not the SDK's
        val packageInfo = manager.getPackageInfo(appContext.packageName, 0)
        val apiVersion = packageInfo.versionName

        // (TODO) val apiVersion = BuildConfig::class.java.getField("versionName").get(null) as String
        val originatorInfo = OriginatorInfo(dapp?.name, dapp?.url, "Android", apiVersion)
        val requestInfo = RequestInfo("originator_info", originatorInfo)
        val requestInfoJson = Gson().toJson(requestInfo)

        Logger.log("CommClient: Sending originator info: $requestInfoJson")

        val payload = keyExchange.encrypt(requestInfoJson)

        val message = Message(sessionId, payload)
        val messageJson = Gson().toJson(message)

        sendMessage(messageJson)
    }

    fun bindService() {
        Logger.log("CommClient: Binding service")

        val serviceIntent = Intent()
            .setComponent(
                ComponentName(
                    "io.metamask",
                    "io.metamask.nativesdk.MessageService"
                )
            )

        if (appContext != null) {
            appContext.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        } else {
            Logger.error("App context null!")
        }
    }

    fun unbindService() {
        if (isServiceConnected) {
            Logger.log("CommClient: Unbinding service")
            appContext.unbindService(serviceConnection)
            isServiceConnected = false
        }
    }

    fun initiateKeyExchange() {
        Logger.log("CommClient: Initiating key exchange")

        val keyExchange = JSONObject().apply {
            put(KeyExchange.PUBLIC_KEY, keyExchange.publicKey)
            put(KeyExchange.TYPE, KeyExchangeMessageType.key_exchange_SYN.name)
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