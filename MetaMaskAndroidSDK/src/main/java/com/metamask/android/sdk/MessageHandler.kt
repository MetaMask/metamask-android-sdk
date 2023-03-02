package com.metamask.android.sdk

import android.annotation.SuppressLint
import android.os.*

class MessageHandler() : Handler(Looper.getMainLooper()) {
    @SuppressLint("SetTextI18n")

    lateinit var receiveMessenger: Messenger
    lateinit var serviceMessenger: Messenger

    companion object {
        const val CONNECTION = 0
        const val KEY_EXCHANGE = 1
        const val MESSAGE = 2

        const val PAYLOAD = "payload"
    }

    private val keyExchange = KeyExchange()

    lateinit var message: Message


    override fun handleMessage(msg: Message) {

        when (msg.what) {
            CONNECTION, KEY_EXCHANGE -> {
                val keyExchangeStep = msg.data.getString(KeyExchange.KEY_EXCHANGE).orEmpty()
                val publicKey= msg.data.getString(KeyExchange.PUBLIC_KEY)

                val currentMessage = KeyExchangeMessage(keyExchangeStep, publicKey)
                val nextKeyExchangeStep = keyExchange.nextKeyExchangeMessage(currentMessage)

                message.data = Bundle().apply {
                    putString(KeyExchange.KEY_EXCHANGE, nextKeyExchangeStep)
                }
            }
            MESSAGE -> {
                val data = msg.data.getString(PAYLOAD)
                message.data = Bundle().apply {

                }
            }
        }

        try {
            message.replyTo = receiveMessenger
            serviceMessenger.send(message)
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
        super.handleMessage(msg)
    }
}