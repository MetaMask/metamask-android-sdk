package com.metamask.android.sdk

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
import io.metamask.IMessegeService
import io.metamask.IMessegeServiceCallback

class CommunicationClient(context: Context, lifecycle: Lifecycle)  {
    private val appContext = context.applicationContext
    private var messageService: IMessegeService? = null
    private val keyExchange = KeyExchange()
    companion object {
        const val TAG = "MM_ANDROID_SDK"
        const val MESSAGE = "message"
        const val KEY_EXCHANGE = "key_exchange"
    }

    private val observer = object : DefaultLifecycleObserver {

        override fun onCreate(owner: LifecycleOwner) {
            Log.d(TAG, "Connecting...")
            val serviceIntent = Intent()
                .setComponent(
                    ComponentName(
                        "io.metamask",
                        "io.metamask.MesssageService"
                    )
                )
            appContext.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        }

        override fun onDestroy(owner: LifecycleOwner) {
            Log.d(TAG, "Disconnecting...")
            appContext.unbindService(serviceConnection)
        }
    }

    init {
        lifecycle.addObserver(observer)
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            messageService = IMessegeService.Stub.asInterface(service)
            messageService?.registerCallback(messageServiceCallback)
            Log.d(MainActivity.TAG,"Service connected $name")
            initiateKeyExchange()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            messageService = null
            Log.e(MainActivity.TAG,"Service connected $name")
        }
    }

    private val messageServiceCallback: IMessegeServiceCallback = object : IMessegeServiceCallback.Stub() {
        override fun onMessageReceived(message: Bundle?) {
            if (message != null) {
                for (key in message.keySet()) {
                    val value = message.get(key)
                    Log.d(TAG, "$key <- $value")
                }
            }

            // Handle the received message
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

    fun sendMessage(message: Bundle) {
        messageService?.sendMessage(message)
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
}