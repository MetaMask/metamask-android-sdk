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
            message?.let { it ->
                it.getBundle(KEY_EXCHANGE)?.let { exchange ->
                    handleKeyExchange(exchange)
                }
                it.getBundle(MESSAGE)?.let { payload ->
                    handleMessage(payload)
                }
            }
        }
    }

    fun sendMessage(message: Bundle) {
        Log.d(TAG, "Sending message:")
        for (key in message.keySet()) {
            val value = message.get(key)
            Log.d(TAG, "$key <- $value")
        }
        messageCallback?.onMessageReceived(message)
    }

    private fun handleKeyExchange(message: Bundle) {
        Log.d(TAG,"Received key exchange")
        for (key in message.keySet()) {
            val value = message.get(key)
            Log.d(TAG, "$key <- $value")
        }

        val step = message.getString(KeyExchange.STEP) ?: KeyExchange.KEY_EXCHANGE_SYN
        val theirPublicKey = message.getString(KeyExchange.PUBLIC_KEY)
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

            response.putString(MESSAGE, "Exchange complete!")
        }

        sendMessage(response)
    }

    private fun handleMessage(message: Bundle) {
        Log.d(TAG, "Received message:")
        for (key in message.keySet()) {
            val value = message.get(key)
            Log.d(TAG, "$key <- $value")
        }

        val response = Bundle()
        response.putBundle(MESSAGE, Bundle())
        sendMessage(response)
    }
}
