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
import org.json.JSONObject
import java.util.UUID

class CommunicationClient(context: Context, lifecycle: Lifecycle)  {
    private val appContext = context.applicationContext
    private var messageService: IMessegeService? = null
    private val keyExchange = KeyExchange()

    lateinit var sessionId: String

    companion object {
        const val TAG = "MM_ANDROID_SDK"
        const val SESSION_ID = "session_id"

        const val MESSAGE = "message"
        const val KEY_EXCHANGE = "key_exchange"
    }

    private val observer = object : DefaultLifecycleObserver {

        override fun onCreate(owner: LifecycleOwner) {
            Log.d(TAG, "CommClient: onCreate()")
            sessionId = UUID.randomUUID().toString()
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
            Log.d(TAG, "CommClient: onMessageReceived!")
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
                it.getString(MESSAGE)?.let { string ->
                    handleMessage(string)
                }
            }
        }
    }

    private fun handleMessage(message: String) {
        Log.d(TAG, "CommClient: Received message: $message")
        val jsonString = keyExchange.decrypt(message)
        val jsonObject = JSONObject(jsonString)
        jsonObject.let {
            // get data
        }

        val response = Bundle().apply {
            val json = JSONObject().apply {
                put(SESSION_ID, sessionId)
            }
            val payload = keyExchange.encrypt(json.toString())
            putString(MESSAGE, payload)
        }

        sendMessage(response)
    }

    private fun handleMessage(message: Bundle) {
        Log.d(TAG, "CommClient: Received message")
        for (key in message.keySet()) {
            val value = message.get(key)
            Log.d(TAG, "$key <- $value")
        }

        val response = Bundle().apply {
            val jsonObject = JSONObject().apply {
                put(SESSION_ID, sessionId)
            }
            val payload = keyExchange.encrypt(jsonObject.toString())
            putString(MESSAGE, payload)
        }
        sendMessage(response)
    }

    private fun handleKeyExchange(message: Bundle) {
        Log.d(TAG,"CommClient: Received key exchange")
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
            // send request_ethAccounts
            response.putString(MESSAGE, "Over & out!!")
        }

        sendMessage(response)
    }

    fun sendMessage(message: Bundle) {
        Log.d(TAG, "Sending message ->")
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
                putString(KeyExchange.TYPE, KeyExchangeMessageType.key_exchange_SYN.name)
            }
            putBundle(KEY_EXCHANGE, bundle)
        }
        sendMessage(message)
    }
}