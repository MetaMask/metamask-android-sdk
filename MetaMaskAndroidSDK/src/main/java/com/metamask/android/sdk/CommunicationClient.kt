package com.metamask.android.sdk

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import io.metamask.IMessegeService
import io.metamask.IMessegeServiceCallback
import kotlinx.coroutines.CompletableDeferred
import org.json.JSONObject
import java.util.UUID
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNull.serializer
import kotlinx.serialization.modules.SerializersModule
import org.json.JSONArray
import java.lang.ref.WeakReference

class CommunicationClient(context: Context, lifecycle: Lifecycle, callback: EthereumEventCallback)  {
    private val appContext = context.applicationContext

    var dapp: Dapp? = null
    lateinit var sessionId: String
    private val isConnected = false
    private var isServiceConnected = false
    private val keyExchange = KeyExchange()
    private var messageService: IMessegeService? = null
    private val ethereumEventCallbackRef: WeakReference<EthereumEventCallback> = WeakReference(callback)

    companion object {
        const val TAG = "MM_ANDROID_SDK"
        const val SESSION_ID = "session_id"

        const val MESSAGE = "message"
        const val DATA = "data"
        const val KEY_EXCHANGE = "key_exchange"
    }

    private var requestJobs: MutableList<() -> Unit> = mutableListOf()
    private var submittedRequests: MutableMap<String, SubmittedRequest<*>>  = mutableMapOf()

    private val observer = object : DefaultLifecycleObserver {

        override fun onCreate(owner: LifecycleOwner) {
            Log.d(TAG, "CommClient: onCreate()")
            sessionId = UUID.randomUUID().toString()
            bindService()
        }

        override fun onStart(owner: LifecycleOwner) {
            Log.d(TAG, "CommClient: onStart()")
        }

        override fun onDestroy(owner: LifecycleOwner) {
            Log.d(TAG, "CommClient: onDestroy: Disconnecting...")
            unbindService()
        }
    }

    init {
        lifecycle.addObserver(observer)
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
        }

        override fun onBindingDied(name: ComponentName?) {
            Log.d(TAG, "CommClient: binding died: $name")
        }

        override fun onNullBinding(name: ComponentName?) {
            Log.d(TAG, "CommClient: null binding: $name")
        }
    }

    private val messageServiceCallback: IMessegeServiceCallback = object : IMessegeServiceCallback.Stub() {
        override fun onMessageReceived(message: String) {
            Log.d(TAG, "CommClient: onMessageReceived!")

            val json = JSONObject(message)
            val keyExchange = json.optString(KEY_EXCHANGE)
            val payload = json.optString(MESSAGE)

            if(keyExchange.isNotEmpty()) {
                handleKeyExchange(keyExchange)
            } else if (payload.isNotEmpty()) {
                handleMessage(payload)
            }
        }
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
            MessageType.READY.value -> {
                Logger.log("Connection ready")
                resumeRequestJobs()
            }
            MessageType.WALLET_INFO.value -> {
                Logger.log("Received wallet info")
            }
            else -> {
                Logger.log("Handling data case")
                json.optJSONObject(MessageType.DATA.value)?.let { data ->
                    val id = data.optString(MessageType.ID.value)

                    if (id.isNotEmpty()) {
                        handleResponse(id, data)
                    } else {
                        handleEvent(data)
                    }
                }
            }
        }
    }

    private fun resumeRequestJobs() {
        while (requestJobs.isNotEmpty()) {
            val job = requestJobs.removeLast()
            job()
        }
    }

    private fun addRequestJob(job: () -> Unit) {
        requestJobs.add { job }
    }

    private fun handleResponse(id: String, data: JSONObject) {
        // check for error
        val errorJson = data.optJSONObject("error")
        if (errorJson != null) {
            val error: Map<String, Any?> = Json.decodeFromString(errorJson.toString())
            val code = error["code"] as? Int ?: -1
            val message = error["message"] as? String ?: ErrorType.message(code)
            completeRequest(id, RequestError(code, message))
            return
        }

        val request = submittedRequests[id]?.request
        val isResultMethod = EthereumMethod.isResultMethod(request?.method ?: "")

        if (!isResultMethod) {
            val resultJson = data.optJSONObject("result")

            if (resultJson != null) {
                val result: Map<String, Serializable> = Json.decodeFromString(resultJson.toString())
                submittedRequests[id]?.deferred?.complete(result)
                completeRequest(id, result)
            } else {
                val result: Map<String, Serializable> = Json.decodeFromString(data.toString())
                completeRequest(id, result)
            }
            return
        }

        when(request?.method) {
            EthereumMethod.GETMETAMASKPROVIDERSTATE.value -> {
                val result = data.optJSONObject("result")
                val accountsArray = result?.optJSONArray("accounts").toString()
                val accounts: List<String> = Json.decodeFromString(accountsArray)

                accounts.getOrNull(0)?.let { account ->
                    updateAccount(account)
                    completeRequest(id, account)
                }

                val chainId = result?.optString("chainId")

                if (chainId != null && chainId.isNullOrEmpty()) {
                    updateChainId(chainId)
                    completeRequest(id, chainId)
                }
            }
            EthereumMethod.ETHREQUESTACCOUNTS.value  -> {
                val resultArray: JSONArray = data.optJSONArray("result") ?: JSONArray()
                val accounts: List<String> = Json.decodeFromString(resultArray.toString())
                val account =  accounts.getOrNull(0)

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

    private fun completeRequest(id: String, result: Any) {
        submittedRequests[id]?.deferred?.complete(result)
        submittedRequests.remove(id)
    }

    private fun handleEvent(event: JSONObject) {

        when (event.optString("method")) {
            EthereumMethod.METAMASKACCOUNTSCHANGED.value -> {
                val accountsArray: JSONArray = event.optJSONArray("params") ?: JSONArray()
                val accounts: List<String> = Json.decodeFromString(accountsArray.toString())

                accounts.getOrNull(0)?.let { account ->
                    updateAccount(account)
                }
            }
            EthereumMethod.METAMASKCHAINCHANGED.value -> {
                val paramsJson = event.optJSONObject("params")
                val chainId = paramsJson?.optString("chainId")

                if (chainId != null && chainId.isNullOrEmpty()) {
                    updateChainId(chainId)
                }
            }
            else -> {
                Logger.error("Unexpected event: $event")
            }
        }
    }

    private fun updateAccount(account: String) {
        val callback = ethereumEventCallbackRef.get()
        callback?.updateAccount(account)
    }

    private fun updateChainId(chainId: String) {
        val callback = ethereumEventCallbackRef.get()
        callback?.updateAccount(chainId)
    }

    private fun handleKeyExchange(message: String) {
        Log.d(TAG,"CommClient: Received key exchange $message")

        val json = JSONObject(message)

        val keyExchangeStep = json.optString(KeyExchange.TYPE, KeyExchangeMessageType.key_exchange_SYN.name)
        val type = KeyExchangeMessageType.valueOf(keyExchangeStep)
        val theirPublicKey = json.optString(KeyExchange.PUBLIC_KEY)
        val keyExchangeMessage = KeyExchangeMessage(type.name, theirPublicKey)
        val nextStep  = keyExchange.nextKeyExchangeMessage(keyExchangeMessage)

        if (nextStep != null) {
            val exchangeMessage = JSONObject().apply {
                val details = JSONObject().apply {
                    put(KeyExchange.PUBLIC_KEY, nextStep.publicKey)
                    put(KeyExchange.TYPE, nextStep.type)
                }
                put(KEY_EXCHANGE, details.toString())
            }

            Logger.log("Sending key exchange message $exchangeMessage")
            sendKeyExchangeMesage(exchangeMessage.toString())
        }

        if (keyExchange.keysExchanged) {
            sendOriginatorInfo()
        }
    }

    fun sendMessage(message: String) {
        Log.d(TAG, "Sending message: $message")

        if (!keyExchange.keysExchanged) {
            addRequestJob { messageService?.sendMessage(message) }
        } else {
            messageService?.sendMessage(message)
        }
    }

    fun <T>sendRequest(request: EthereumRequest<T>, deferred: CompletableDeferred<Any>) {
        Logger.log("CommClient: sending request $request")
        val requestString = Json.encodeToString(request)
        Logger.log("CommClient: requestString $requestString")

        val message = JSONObject().apply {
            val details = JSONObject().apply {
                put(SESSION_ID, sessionId)
                put(DATA, Json.encodeToString(request))
            }
            val payload = keyExchange.encrypt(details.toString())
            put(MESSAGE, payload)
        }

        submittedRequests[request.id] = SubmittedRequest(request, deferred)
        sendMessage(message.toString())
    }

    private fun sendOriginatorInfo() {
        Logger.log("Sending originator info")
        val manager = appContext.packageManager
        val packageInfo = manager.getPackageInfo(appContext.packageName, 0)
        val apiVersion = packageInfo.versionName
        Logger.log("apiVersion info is $apiVersion")

        //val apiVersion = BuildConfig::class.java.getField("versionName").get(null) as String
        val originatorInfo = OriginatorInfo(dapp?.name, dapp?.url, "Android", apiVersion)
        val requestInfo = RequestInfo("originator_info", originatorInfo)

        val message = JSONObject().apply {
            val details = JSONObject().apply {
                put(SESSION_ID, sessionId)
                put(DATA, Json.encodeToString(requestInfo))
            }
            val payload = keyExchange.encrypt(details.toString())
            put(MESSAGE, payload)
        }

        sendMessage(message.toString())
    }

    fun bindService() {
        Log.d(TAG, "Binding service")
        val serviceIntent = Intent()
            .setComponent(
                ComponentName(
                    "com.reactwallet",
                    "com.reactwallet.MessageService"
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
            Log.d(TAG, "Unbinding service")
            appContext.unbindService(serviceConnection)
            isServiceConnected = false
        }
    }

    private fun initiateKeyExchange() {
        Log.d(TAG, "CommClient: Initiating key exchange")
        val json = JSONObject().apply {
            val keyExchange = JSONObject().apply {
                put(KeyExchange.PUBLIC_KEY, keyExchange.publicKey)
                put(KeyExchange.TYPE, KeyExchangeMessageType.key_exchange_SYN.name)
            }
            put(KEY_EXCHANGE, keyExchange.toString())
        }
        Logger.log("Sending exchange $json")
        sendKeyExchangeMesage(json.toString())
    }

    private fun sendKeyExchangeMesage(message: String) {
        messageService?.sendMessage(message)
    }
}