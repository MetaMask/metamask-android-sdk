package com.reactwallet

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import io.metamask.IMessegeService
import io.metamask.IMessegeServiceCallback
import org.json.JSONObject

class MessageService : Service() {
    private val messageServiceCallbacks = mutableListOf<IMessegeServiceCallback>()

    companion object {
        const val TAG = "AIDL_NATIVE_MODULE"
        const val MESSAGE = "message"
        const val KEY_EXCHANGE = "key_exchange"
        const val SESSION_ID = "session_id"
    }

    override fun onCreate() {
        super.onCreate()
        registerReceiver()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver()
    }

    override fun onBind(p0: Intent?): IBinder? {
        Log.d(TAG, "MessageService: onBind called!")
        return binder
    }

    private val binder = object : IMessegeService.Stub() {
        private var channelId: String? = null

        override fun registerCallback(callback: IMessegeServiceCallback?) {
            Log.d(TAG, "MessageService: Callback registered!")
            callback?.let {
                messageServiceCallbacks.add(it)
            }
        }

        override fun sendMessage(message: Bundle) {
            val keyExchange = message.getString(KEY_EXCHANGE)
            val sessionId = message.getString(SESSION_ID)
            if (sessionId != null && channelId.isNullOrEmpty()) {
                channelId = sessionId
                broadcastEvent(EventType.CLIENTS_CONNECTED,null, sessionId)
            }

            val message = message.getString(MESSAGE)

            if (keyExchange != null) {
                handleKeyExchange(keyExchange)
            } else if (message != null) {
                handleMessage(message, sessionId)
            }
        }
    }

    fun sendMessage(message: String) {
        Log.d(TAG, "MessageService: Sending message: $message")
        val message = Bundle().apply {
            putString(MESSAGE, message)
        }
        messageServiceCallbacks.forEach {
            it.onMessageReceived(message)
        }
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

            Log.d(TAG, "Sending next key exchange message $exchangeMessage")

            sendKeyExchangeMesage(exchangeMessage)
        }

        if (
            type == KeyExchangeMessageType.key_exchange_SYNACK ||
            type == KeyExchangeMessageType.key_exchange_ACK) {
            KeyExchange.complete()

            val ready = JSONObject().apply {
                put("type", "ready")
            }.toString()

            Log.d(TAG, "Sending ready: $ready")
            val payload = KeyExchange.encrypt(ready)
            sendMessage(payload)

            broadcastEvent(EventType.KEYS_EXCHANGED,null, null)
        }
    }

    // Broadcasts event to CommunicationClient
    private fun broadcastMessage(eventType: EventType, data: String? = null, sessionId: String?) {
        val intent = Intent("local.client")
        intent.putExtra(eventType.value, data)
        if (sessionId != null) {
            intent.putExtra(SESSION_ID, sessionId)
        }
        applicationContext.sendBroadcast(intent)
    }

    private fun broadcastEvent(eventType: EventType, data: String? = null, sessionId: String?) {
        val intent = Intent("local.client")
        intent.putExtra("event", eventType.value)

        if (data != null) {
            intent.putExtra("data", data)
        }

        if (sessionId != null) {
            intent.putExtra(SESSION_ID, sessionId)
        }

        applicationContext.sendBroadcast(intent)
    }

    private fun sendKeyExchangeMesage(message: String) {
        Log.d(TAG, "Sending key exchange: $message")
        val bundle = Bundle().apply {
            putString(KEY_EXCHANGE, message)
        }
        messageServiceCallbacks.forEach {
            it.onMessageReceived(bundle)
        }
    }

    private fun handleMessage(message: String, sessionId: String?) {
        val payload = KeyExchange.decrypt(message)
        Log.d(TAG, "MessageService (handleMessage): Received message: $payload")

        broadcastMessage(EventType.MESSAGE, payload, sessionId)
    }

    private fun registerReceiver() {
        Log.d(TAG, "MessageService: Registering broadcast receiver")
        val intentFilter = IntentFilter("local.service")
        applicationContext.registerReceiver(broadcastReceiver, intentFilter)
    }

    private fun unregisterReceiver() {
        Log.d(TAG, "MessageService: Deregistering broadcast receiver")
        applicationContext.unregisterReceiver(broadcastReceiver)
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "local.service") {
                val message = intent.getStringExtra(EventType.MESSAGE.value) as String
                Log.d(TAG,"MessegeService: Got broadcast message: $message")

                val bundle = Bundle().apply {
                    putString(EventType.MESSAGE.value, message)
                }

                messageServiceCallbacks.forEach {
                    it.onMessageReceived(bundle)
                }
            }
        }
    }
}
