package io.metamask.nativesdk

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.util.Log
import org.json.JSONObject

class MessageService : Service(), ConnectionStatusCallback {
    private var dappMessageServiceCallback: IMessegeServiceCallback? = null
    private var jobQueue: MutableList<() -> Unit> = mutableListOf()

    private var sessionId: String = ""
    private val tracker: Tracker = Analytics()
    private var isConnected: Boolean = false
    private var sentOriginatorInfo = false
    private val connectionStatusManager: ConnectionStatusManager = ConnectionStatusManager.getInstance()

    override fun onCreate() {
        super.onCreate()
        registerBroadcastReceiver()
        connectionStatusManager.addCallback(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        Logger.log("MessageService: onDestroy")
        unregisterBroadcastReceiver()
    }

    override fun onBind(p0: Intent?): IBinder? {
        Logger.log("MessageService: onBind")
        return binder
    }

    override fun onConnect() {
        Logger.log("MessageService: onConnect")
        isConnected = true
        Handler().postDelayed({
            resumeQueuedJobs()
        }, 2000)

    }

    override fun onDisconnect() {
        Logger.log("MessageService: onDisconnect")
        isConnected = false
    }

    private val binder = object : IMessegeService.Stub() {

        override fun registerCallback(callback: IMessegeServiceCallback?) {

            if (callback.toString().contains("io.metamask.nativesdk")) {
                Logger.log("MessageService: Initialising dapp callback")
                dappMessageServiceCallback = callback
                Logger.log("MessageService: Dapp callback registered - $callback")
            } else {
                Logger.log("MessageService: Metamask callback registered - $callback")
            }
        }

        override fun sendMessage(message: Bundle) {
            val keyExchange = message.getString(KEY_EXCHANGE)
            val messageJson = message.getString(MESSAGE)

            if (messageJson != null) {
                Logger.log("MessageService: Got message $messageJson")
                val messageJsonObject = JSONObject(messageJson)
                val id = messageJsonObject.optString("id")
                val message = messageJsonObject.optString("message")

                sessionId = id

                handleMessage(message)
            } else if (keyExchange != null) {
                if (KeyExchange.keysExchanged) {
                    KeyExchange.resetKeys()
                }

                handleKeyExchange(keyExchange)
            }
        }
    }

    private fun sendMessage(message: String) {
        Logger.log("MessageService: Sending message to dapp -> $message")
        val message = Bundle().apply {
            putString(MESSAGE, message)
        }
        dappMessageServiceCallback?.onMessageReceived(message)
    }

    private fun handleKeyExchange(message: String) {
        Log.d(TAG,"MessageService: Received key exchange $message")
        val json = JSONObject(message)

        val keyExchangeStep = json.optString(KeyExchange.TYPE, KeyExchangeMessageType.key_exchange_SYN.name)
        val type = KeyExchangeMessageType.valueOf(keyExchangeStep)
        val theirPublicKey = json.optString(KeyExchange.PUBLIC_KEY)
        val keyExchangeMessage = KeyExchangeMessage(type.name, theirPublicKey) //Json.decodeFromString(message)
        val nextStep  = KeyExchange.nextKeyExchangeMessage(keyExchangeMessage)

        if (nextStep != null) {
            val exchangeMessage = JSONObject().apply {
                put(KeyExchange.PUBLIC_KEY, nextStep.publicKey)
                put(KeyExchange.TYPE, nextStep.type)
            }.toString()

            Logger.log("Sending next key exchange message $exchangeMessage")

            sendKeyExchangeMessage(exchangeMessage)
        }

        if (
            type == KeyExchangeMessageType.key_exchange_SYNACK ||
            type == KeyExchangeMessageType.key_exchange_ACK) {
            KeyExchange.complete()

            val keysExchangedMessage = JSONObject().apply {
                put("type", EventType.KEYS_EXCHANGED.value)
            }.toString()

            Logger.log("MessageService: $keysExchangedMessage")
            val payload = KeyExchange.encrypt(keysExchangedMessage)
            sendMessage(payload)
        }
    }

    // Broadcasts event to CommunicationClient
    private fun broadcastEvent(eventType: EventType, data: String) {
        val intent = Intent("local.client")
        intent.putExtra("event", eventType.value)
        intent.putExtra("data", data)

        if (isConnected) {
            broadcastEvent(intent)
        } else {
            queueJob { broadcastEvent(intent) }
        }
    }

    private fun broadcastEvent(intent: Intent) {
        applicationContext.sendBroadcast(intent)
    }

    private fun queueJob(job: () -> Unit) {
        Logger.log("Queue job")
        jobQueue.add(job)
    }

    private fun resumeQueuedJobs() {
        Logger.log("Resuming jobs")

        while (jobQueue.isNotEmpty()) {
            Logger.log("Running job")
            val job = jobQueue.removeFirstOrNull()
            job?.invoke()
        }
    }

    private fun sendKeyExchangeMessage(message: String) {
        Logger.log("Sending key exchange: $message")
        val bundle = Bundle().apply {
            putString(KEY_EXCHANGE, message)
        }
        dappMessageServiceCallback?.onMessageReceived(bundle)
    }

    private fun handleMessage(message: String) {
        val payload = KeyExchange.decrypt(message)
        Logger.log("MessageService: (handleMessage) payload $payload")

        val payloadJsonObject = JSONObject(payload)
        val isOriginatorInfo = payloadJsonObject.optString("originatorInfo").isNotEmpty()

        if (isOriginatorInfo) {
            Log.d(TAG,"Sending originator info")
            payloadJsonObject.apply {
                put("clientId", sessionId)
            }

            sentOriginatorInfo = true
            broadcastEvent(EventType.CLIENTS_CONNECTED, payloadJsonObject.toString())
            trackEvent(Event.CONNECTED, sessionId)
        } else {
            val message = JSONObject().apply {
                put("id", sessionId)
                put("message", payload)
            }.toString()

            broadcastEvent(EventType.MESSAGE, message)
        }
    }

    private fun trackEvent(event: Event, id: String) {
        val params: MutableMap<String, String> = mutableMapOf(
            "id" to id
        )

        tracker.trackEvent(event, params)
    }

    private fun registerBroadcastReceiver() {
        Logger.log("MessageService: registerBroadcastReceiver")
        val intentFilter = IntentFilter("local.service")
        applicationContext.registerReceiver(broadcastReceiver, intentFilter)
    }

    private fun unregisterBroadcastReceiver() {
        Logger.log("MessageService: unregisterBroadcastReceiver")
        applicationContext.unregisterReceiver(broadcastReceiver)
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "local.service") {
                val message = intent.getStringExtra(EventType.MESSAGE.value) as String
                Log.d(TAG,"MessageService: Got broadcast message: $message")

                val bundle = Bundle().apply {
                    putString(EventType.MESSAGE.value, message)
                }

                dappMessageServiceCallback?.onMessageReceived(bundle)
            }
        }
    }
}