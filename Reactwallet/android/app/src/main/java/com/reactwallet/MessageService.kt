package com.reactwallet

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import io.metamask.IMessegeService
import io.metamask.IMessegeServiceCallback

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

        override fun sendMessage(message: Bundle?) {
            Log.d(TAG, "MessageService: Received message!")
            message?.let {
                for (key in it.keySet()) {
                    val value = it.get(key)
                    Log.d(TAG, "$key <- $value")
                }
            }

            message?.let {
                it.getBundle(KEY_EXCHANGE)?.let { exchange ->
                    handleKeyExchange(exchange)
                }

                it.getBundle(MESSAGE)?.let {  message ->
                    handleMessage(message)
                }

                it.getString(MESSAGE)?.let { string ->
                    handleMessage(string)
                }
            }
        }
    }

    fun sendMessage(message: Bundle) {
        Log.d(TAG, "MessageService: Sending message:")
        for (key in message.keySet()) {
            val value = message.get(key)
            Log.d(TAG, "$key <- $value")
        }
        messageServiceCallbacks.forEach {
            it.onMessageReceived(message)
        }
        //messageCallback?.onMessageReceived(message)
    }

    private fun handleKeyExchange(message: Bundle) {
        Log.d(TAG,"MessageService: Received key exchange")
        for (key in message.keySet()) {
            val value = message.get(key)
            Log.d(TAG, "$key <- $value")
        }

        val keyExchangeStep = message.getString(KeyExchange.TYPE) ?: KeyExchangeMessageType.key_exchange_SYN.name
        val type = KeyExchangeMessageType.valueOf(keyExchangeStep)
        val theirPublicKey = message.getString(KeyExchange.PUBLIC_KEY)
        val keyExchangeMessage = KeyExchangeMessage(type, theirPublicKey)
        val nextStep  = keyExchange.nextKeyExchangeMessage(keyExchangeMessage)

        val response = Bundle()

        nextStep?.let {
            val bundle = Bundle().apply {
                putString(KeyExchange.PUBLIC_KEY, it.publicKey)
                putString(KeyExchange.TYPE, it.type.name)
            }
            response.putBundle(KEY_EXCHANGE, bundle)
        } ?: run {
            response.putString(MESSAGE, "Done!")
        }

        sendMessage(response)
    }

    private fun handleMessage(message: String) {
        Log.d(TAG, "MessageService: Received message $message")

        Log.d(TAG, "MessageService: Broadcasting message...")
        val intent = Intent("com.reactwallet.MESSAGE")
        intent.putExtra(MESSAGE, message)
        applicationContext.sendBroadcast(intent)
    }

    private fun handleMessage(message: Bundle) {
        Log.d(TAG, "MessageService: Received message:")
        for (key in message.keySet()) {
            val value = message.get(key)
            Log.d(TAG, "$key <- $value")
        }

        messageServiceCallbacks.forEach { callback ->
            callback.onMessageReceived(message)
        }

        Log.d(TAG, "MessageService: Broadcasting message...")
        val intent = Intent("com.reactwallet.MESSAGE")
        intent.putExtra(MESSAGE, message)
        applicationContext.sendBroadcast(intent)
    }
}
