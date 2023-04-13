package com.reactwallet

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import io.metamask.IMessegeService
import io.metamask.IMessegeServiceCallback

class MessageService : Service() {
    private var messageCallback: IMessegeServiceCallback? = null
    private val keyExchange = KeyExchange()

    companion object {
        const val TAG = "MM_MOBILE"
        const val MESSAGE = "message"
        const val KEY_EXCHANGE = "key_exchange"
    }

    override fun onBind(p0: Intent?): IBinder? {
        Log.d(TAG, "onBind called!")
        return binder
    }

    private val binder = object : IMessegeService.Stub() {
        override fun registerCallback(callback: IMessegeServiceCallback?) {
            Log.d(TAG, "Callback registered!")
            messageCallback = callback
        }

        override fun sendMessage(message: Bundle?) {
            if (message != null) {
                for (key in message.keySet()) {
                    val value = message.get(key)
                    Log.d(TAG, "$key <- $value")
                }
            }

            // Handle the received key exchange
            val keyExchange = message?.getBundle(KEY_EXCHANGE)
            keyExchange?.let {
                handleKeyExchange(it)
            }

            // Handle the received message
            val payload = message?.getBundle(MESSAGE)
            payload?.let {
                handleMessage(it)
            }
        }
    }

    fun sendMessage(message: Bundle) {
        messageCallback?.onMessageReceived(message)
    }

    private fun initiateKeyExchange() {
        Log.d(TAG, "Initiating key exchange")
        val message = Bundle().apply {
            val bundle = Bundle().apply {
                putString(KeyExchange.STEP, KeyExchange.KEY_EXCHANGE_SYN)
            }
            putBundle(KEY_EXCHANGE, bundle)
        }
        sendMessage(message)
    }

    private fun handleKeyExchange(bundle: Bundle) {
        val step = bundle.getString(KeyExchange.STEP) ?: KeyExchange.KEY_EXCHANGE_SYN
        val theirPublicKey = bundle.getString(KeyExchange.PUBLIC_KEY)
        val keyExchangeMessage = KeyExchangeMessage(step, theirPublicKey)
        val nextStep  = keyExchange.nextKeyExchangeMessage(keyExchangeMessage)

        val response = Bundle()

        nextStep?.let {
            val bundle = Bundle().apply {
                putString(KeyExchange.PUBLIC_KEY, it.publicKey)
                putString(KeyExchange.STEP, it.step)
            }
            response.putBundle(KEY_EXCHANGE, bundle)
        } ?: run {
            // Send originator info
            response.putBundle(MESSAGE, Bundle())
        }

        sendMessage(bundle)
    }

    private fun handleMessage(message: Bundle) {
        Log.d(TAG, "Received message")
        for (key in message.keySet()) {
            val value = message.get(key)
            Log.d(TAG, "$key <- $value")
        }

        val response = Bundle()
        response.putBundle(MESSAGE, Bundle())
        sendMessage(response)
    }
}
