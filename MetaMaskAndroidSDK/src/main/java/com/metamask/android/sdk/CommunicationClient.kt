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
        const val MESSAGE = "MESSAGE"
        const val KEY_EXCHANGE = "KEY_EXCHANGE"
    }

    private val observer = object : DefaultLifecycleObserver {

        override fun onCreate(owner: LifecycleOwner) {
            Log.d(TAG, "CommClient: onCreate()")
            bindService()
        }

        override fun onStart(owner: LifecycleOwner) {
            Log.d(TAG, "CommClient: onStart()")
        }

        override fun onDestroy(owner: LifecycleOwner) {
            Log.d(TAG, "CommClient: onDestroy: Disconnecting...")
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
            Log.d(TAG,"CommClient: Service connected $name")
            initiateKeyExchange()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            messageService = null
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
        override fun onMessageReceived(message: Bundle?) {
            if (message != null) {
                for (key in message.keySet()) {
                    val value = message.get(key)
                    Log.d(TAG, "$key <- $value")
                }
            }

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

    private fun handleMessage(message: Bundle) {
        Log.d(TAG, "Received message")
        for (key in message.keySet()) {
            val value = message.get(key)
            Log.d(TAG, "$key <- $value")
        }
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

            response.putString(MESSAGE, "Over & out!!")
        }

        sendMessage(response)
    }

    fun sendMessage(message: Bundle) {
        Log.d(TAG, "Sending message:")
        for (key in message.keySet()) {
            val value = message.get(key)
            Log.d(TAG, "$key <- $value")
        }
        messageService?.sendMessage(message)
    }
    fun bindService() {
        Log.d(TAG, "Binding service now!")
        val serviceIntent = Intent()
            .setComponent(
                ComponentName(
                    "com.reactwallet",
                    "com.reactwallet.MessageService"
                )
            )
        if (appContext != null) {
            Log.d(TAG, "App context all good")
            appContext.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        } else {
            Log.d(TAG, "App context null!")
        }
    }
    private fun initiateKeyExchange() {
        Log.d(TAG, "CommClient: Initiating key exchange")
        val message = Bundle().apply {
            val bundle = Bundle().apply {
                putString(KeyExchange.STEP, KeyExchange.KEY_EXCHANGE_SYN)
            }
            putBundle(KEY_EXCHANGE, bundle)
        }
        sendMessage(message)
    }
}