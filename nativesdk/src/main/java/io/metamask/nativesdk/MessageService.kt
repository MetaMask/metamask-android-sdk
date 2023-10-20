package io.metamask.nativesdk

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.IBinder
import org.json.JSONObject

class MessageService : Service(), MetaMaskConnectionStatusCallback {
    private var dappMessageServiceCallback: IMessegeServiceCallback? = null
    private var jobQueue: MutableList<() -> Unit> = mutableListOf()

    private var sessionId = ""
    private var sentOriginatorInfo = false
    private var isMetaMaskReady: Boolean = false
    private var isMetaMaskBroadcastRegistered: Boolean = false
    private var isMetaMaskBoundServiceConnected: Boolean = false

    private var dappOriginatorInfo: String? = null

    private val _keyExchange = KeyExchange.getInstance()

    private val keyExchange: KeyExchange get() {
        synchronized(this) {
            return _keyExchange
        }
    }
    private val connectionStatusManager: ConnectionStatusManager = ConnectionStatusManager.getInstance()

    private fun setIsMetaMaskBoundServiceConnected(newValue: Boolean) {
        synchronized(this) {
            isMetaMaskBoundServiceConnected = newValue
        }
    }

    private fun metaMaskBroadcastRegistered(): Boolean {
        synchronized(this) {
            return isMetaMaskBroadcastRegistered
        }
    }

    private fun setIsMetaMaskBroadcastRegistered(newValue: Boolean) {
        synchronized(this) {
            isMetaMaskBroadcastRegistered = newValue
        }
    }

    private fun metaMaskBoundServiceConnected(): Boolean {
        synchronized(this) {
            return isMetaMaskBoundServiceConnected
        }
    }

    private fun setIsMetaMaskReady(newValue: Boolean) {
        synchronized(this) {
            isMetaMaskReady = newValue
        }
    }

    private fun metaMaskReady(): Boolean {
        synchronized(this) {
            return isMetaMaskReady
        }
    }

    override fun onCreate() {
        super.onCreate()
        registerBroadcastReceiver()
        connectionStatusManager.setCallback(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        Logger.log("MessageService:: onDestroy")
        unregisterBroadcastReceiver()
    }

    override fun onBind(p0: Intent?): IBinder? {
        Logger.log("MessageService:: onBind")
        return binder
    }

    override fun onMetaMaskConnect() {
        Logger.log("MessageService:: onMetaMaskConnect")
        setIsMetaMaskBoundServiceConnected(true)
        resumeQueuedJobs()
    }

    override fun onMetaMaskReady() {
        Logger.log("MessageService:: onMetaMaskReady")

        setIsMetaMaskReady(true)
        resumeQueuedJobs()
    }

    override fun onMetaMaskBroadcastRegistered() {
        Logger.log("MessageService:: onMetaMaskBroadcastRegistered")
        setIsMetaMaskBroadcastRegistered(true)
    }

    override fun onMetaMaskBroadcastUnregistered() {
        Logger.log("MessageService:: onMetaMaskBroadcastUnregistered")
        setIsMetaMaskBroadcastRegistered(false)
    }

    override fun onMetaMaskDisconnect() {
        Logger.log("MessageService:: onMetaMaskDisconnect")
        setIsMetaMaskBoundServiceConnected(false)
        trackEvent(Event.SDK_DISCONNECTED, SessionManager.getInstance().sessionId)
    }

    private val binder = object : IMessegeService.Stub() {

        override fun registerCallback(callback: IMessegeServiceCallback?) {

            if (callback.toString().contains("io.metamask.nativesdk")) {
                dappMessageServiceCallback = callback
                Logger.log("MessageService:: Dapp callback registered - $callback")
            } else {
                Logger.log("MessageService:: Metamask callback registered - $callback")
            }
        }

        override fun sendMessage(bundle: Bundle) {

            val keyExchangeJson = bundle.getString(KEY_EXCHANGE)
            val messageJson = bundle.getString(MESSAGE)

            if (keyExchangeJson != null) {
                handleKeyExchange(keyExchangeJson)
            } else if (messageJson != null) {
                Logger.log("MessageService::sendMessage rx from dapp")
                val messageJsonObject = JSONObject(messageJson)
                val id = messageJsonObject.optString("id")
                val message = messageJsonObject.optString("message")

                if (metaMaskBoundServiceConnected() || metaMaskReady() || metaMaskBroadcastRegistered()) {
                    handleMessage(message, id)
                } else {
                    if (!keyExchange.keysExchanged()) {
                        Logger.log("MessageService:: keys not exchanged, request new key exchange")
                        initiateKeyExchange()
                    } else {
                        Logger.log("MessageService:: SDK not yet connected to wallet, sending request to connect")
                        val intent = Intent("local.client").apply {
                            putExtra("event", EventType.BIND.value)
                        }
                        broadcastEvent(intent)
                    }

                    if (metaMaskReady()) {
                        handleMessage(message, id)
                    } else {
                        Logger.log("MessageService:: queueing job while waiting for SDK to connect to wallet")
                        queueJob { handleMessage(message, id) }
                    }
                }
            }
        }
    }

    private fun sendMessage(message: String) {
        val bundle = Bundle().apply {
            putString(MESSAGE, message)
        }
        dappMessageServiceCallback?.onMessageReceived(bundle)
    }

    private fun handleKeyExchange(message: String) {
        Logger.log("MessageService:: Received key exchange $message")
        val json = JSONObject(message)

        val keyExchangeStep = json.optString(KeyExchange.TYPE, KeyExchangeMessageType.KEY_HANDSHAKE_SYN.name)
        val type = KeyExchangeMessageType.valueOf(keyExchangeStep)
        val theirPublicKey = json.optString(KeyExchange.PUBLIC_KEY)
        val keyExchangeMessage = KeyExchangeMessage(type.name, theirPublicKey) //Json.decodeFromString(message)
        val nextStep = keyExchange.nextKeyExchangeMessage(keyExchangeMessage)

        if (nextStep != null) {
            val exchangeMessage = JSONObject().apply {
                put(KeyExchange.PUBLIC_KEY, nextStep.publicKey)
                put(KeyExchange.TYPE, nextStep.type)
            }.toString()

            sendKeyExchangeMessage(exchangeMessage)
        }

        if (
            type == KeyExchangeMessageType.KEY_HANDSHAKE_SYNACK ||
            type == KeyExchangeMessageType.KEY_HANDSHAKE_ACK) {
            keyExchange.complete()

            val keysExchangedMessage = JSONObject().apply {
                put("type", EventType.KEYS_EXCHANGED.value)
            }.toString()

            Logger.log("MessageService:: $keysExchangedMessage")
            val payload = keyExchange.encrypt(keysExchangedMessage)
            sendMessage(payload)
        }
    }

    fun initiateKeyExchange() {
        Logger.log("MessageService:: Initiating key exchange")

        val keyExchangeMessage = JSONObject().apply {
            put(KeyExchange.PUBLIC_KEY, keyExchange.publicKey)
            put(KeyExchange.TYPE, KeyExchangeMessageType.KEY_HANDSHAKE_START.name)
        }.toString()

        sendKeyExchangeMessage(keyExchangeMessage)
    }

    // Broadcasts event to CommunicationClient
    private fun broadcastEvent(eventType: EventType, data: String) {
        val intent = Intent("local.client").apply {
            putExtra("event", eventType.value)
            putExtra("data", data)
        }

        if (metaMaskBoundServiceConnected() || metaMaskReady()) {
            broadcastEvent(intent)
        } else {
            queueJob { broadcastEvent(intent) }
        }
    }

    private fun requestBindMetamaskService() {
        Logger.log("MessageService:: Requesting connection to wallet")

        val intent = Intent("local.client")
        intent.putExtra("event", EventType.BIND.value)
        broadcastEvent(intent)
    }

    private fun broadcastEvent(intent: Intent) {
        applicationContext.sendBroadcast(intent)
    }

    private fun queueJob(job: () -> Unit) {
        Logger.log("MessageService:: Queue job")
        jobQueue.add(job)
    }

    private fun resumeQueuedJobs() {
        Logger.log("MessageService:: Resuming jobs")

        while (jobQueue.isNotEmpty()) {
            val job = jobQueue.removeFirstOrNull()
            job?.invoke()
        }
    }

    private fun sendKeyExchangeMessage(message: String) {
        Logger.log("MessageService:: Sending key exchange: $message")
        val bundle = Bundle().apply {
            putString(KEY_EXCHANGE, message)
        }
        dappMessageServiceCallback?.onMessageReceived(bundle)
    }

    private fun handleMessage(message: String, id: String) {
        val payload = keyExchange.decrypt(message)

        if (id != sessionId) {
            sessionId = id
            SessionManager.getInstance().sessionId = id
        }

        val payloadJsonObject = JSONObject(payload)
        val isOriginatorInfo = payloadJsonObject.optString("originatorInfo").isNotEmpty()

        if (isOriginatorInfo) {
            sessionId = id

            payloadJsonObject.apply {
                put("clientId", sessionId)
            }
            dappOriginatorInfo = payloadJsonObject.toString()

            sendOriginatorInfo()
        } else {
            val messageJSON = JSONObject().apply {
                put("id", id)
                put("message", payload)
            }.toString()

            if (keyExchange.keysExchanged()) {
                broadcastEvent(EventType.MESSAGE, messageJSON)
            } else {
                Logger.log("MessageService:: Keys not exchanged, initiating key exchange and queueing request $messageJSON")
                queueJob { broadcastEvent(EventType.MESSAGE, messageJSON) }
                initiateKeyExchange()
            }
        }
    }

    private fun sendOriginatorInfo() {
        Logger.log("MessageService:: CLIENTS_CONNECTED, originator info $dappOriginatorInfo")
        sentOriginatorInfo = true
        broadcastEvent(EventType.CLIENTS_CONNECTED, dappOriginatorInfo ?: "")
        trackEvent(Event.SDK_CONNECTION_ESTABLISHED, sessionId)
        resumeQueuedJobs()
    }

    private fun trackEvent(event: Event, id: String) {
        val params: MutableMap<String, String> = mutableMapOf(
            "id" to id
        )

        Analytics.trackEvent(event, params)
    }

    private fun registerBroadcastReceiver() {
        Logger.log("MessageService:: registerBroadcastReceiver")
        val intentFilter = IntentFilter("local.service")
        applicationContext.registerReceiver(broadcastReceiver, intentFilter)
    }

    private fun unregisterBroadcastReceiver() {
        Logger.log("MessageService:: unregisterBroadcastReceiver")
        applicationContext.unregisterReceiver(broadcastReceiver)
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "local.service") {
                val message = intent.getStringExtra(EventType.MESSAGE.value) as String

                val bundle = Bundle().apply {
                    putString(EventType.MESSAGE.value, message)
                }

                dappMessageServiceCallback?.onMessageReceived(bundle)
            }
        }
    }
}