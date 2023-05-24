package com.reactwallet

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import io.metamask.IMessegeService
import io.metamask.IMessegeServiceCallback
import org.json.JSONObject
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

class MessageService : Service() {
    private val messageServiceCallbacks = mutableListOf<IMessegeServiceCallback>()
    private val keyExchange = KeyExchange()

    companion object {
        const val TAG = "MM_MOBILE"
        const val MESSAGE = "message"
        const val KEY_EXCHANGE = "key_exchange"
    }

    override fun onBind(p0: Intent?): IBinder? {
        Log.d(TAG, "MessageService: onBind called!")
        return binder
    }

    private val binder = object : IMessegeService.Stub() {
        override fun registerCallback(callback: IMessegeServiceCallback?) {
            Log.d(TAG, "MessageService: Callback registered!")
            callback?.let {
                messageServiceCallbacks.add(it)
            }
        }

        override fun sendMessage(message: String) {
            Log.d(TAG, "MessageService(sendMessage): Received message: $message")
            val messageJson = JSONObject(message)
            val keyExchange = messageJson.optString(KEY_EXCHANGE)
            val data = messageJson.optString(MESSAGE)

            if (keyExchange.isNotEmpty()) {
                handleKeyExchange(keyExchange)
            } else if (data.isNotEmpty()) {
                handleMessage(data)
            }
        }
    }

    fun sendMessage(message: String) {
        Log.d(TAG, "MessageService: Sending message: $message")
        messageServiceCallbacks.forEach {
            it.onMessageReceived(message)
        }
    }

    private fun handleKeyExchange(message: String) {
        Log.d(TAG,"MessageService: Received key exchange $message")
        val json = JSONObject(message)
        Log.d(TAG,"MessageService: json $json")

        val keyExchangeStep = json.optString(KeyExchange.TYPE, KeyExchangeMessageType.key_exchange_SYN.name)
        val type = KeyExchangeMessageType.valueOf(keyExchangeStep)
        val theirPublicKey = json.optString(KeyExchange.PUBLIC_KEY)
        val keyExchangeMessage = KeyExchangeMessage(type.name, theirPublicKey) //Json.decodeFromString(message)
        val nextStep  = keyExchange.nextKeyExchangeMessage(keyExchangeMessage)

        if (nextStep != null) {
            val exchangeMessage = JSONObject().apply {
                val details = JSONObject().apply {
                    put(KeyExchange.PUBLIC_KEY, nextStep.publicKey)
                    put(KeyExchange.TYPE, nextStep.type)
                }
                put(KEY_EXCHANGE, details.toString())
            }

            Log.d(TAG, "Sending key exchange message $exchangeMessage")

            sendKeyExchangeMesage(exchangeMessage.toString())
        } else {
            val response = JSONObject().apply {
                val walletInfo = JSONObject().apply {
                    put("type", "ready")
                }
                val payload = keyExchange.encrypt(walletInfo.toString())
                put(MESSAGE, payload)
            }
            sendMessage(response.toString())
        }
    }

    private fun sendKeyExchangeMesage(message: String) {
        Log.d(TAG, "Sending key exchange: $message")
        messageServiceCallbacks.forEach {
            it.onMessageReceived(message)
        }
    }

    private fun handleMessage(message: String) {
        val payload = keyExchange.decrypt(message)
        Log.d(TAG, "MessageService (handleMessage): Received message: $payload")

//        messageServiceCallbacks.forEach { callback ->
//            callback.onMessageReceived(message)
//        }

        Log.d(TAG, "MessageService: Broadcasting message...")
        val intent = Intent("com.reactwallet.MESSAGE")
        intent.putExtra(MESSAGE, payload)
        applicationContext.sendBroadcast(intent)
    }
}
