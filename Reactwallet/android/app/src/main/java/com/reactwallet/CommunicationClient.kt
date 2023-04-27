package com.reactwallet

import android.app.Service
import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import com.facebook.react.bridge.*

import io.metamask.IMessegeService
import io.metamask.IMessegeServiceCallback
import java.text.SimpleDateFormat
import java.util.*

class CommunicationClient(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    private val context = reactContext

    override fun getName() = "CommunicationClient"

    companion object {
        const val TAG = "MM_MOBILE"
        const val MESSAGE = "message"
        const val MESSAGE_TYPE = "message_type"
    }

    private var isServiceConnected = false
    private var messageService: IMessegeService? = null
    var messageCallback: IMessegeServiceCallback? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            messageService = IMessegeService.Stub.asInterface(service)
            isServiceConnected = true
            Log.d(TAG,"Service connected $name")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            messageService = null
            isServiceConnected = false
            Log.e(TAG,"Service disconnected $name")
        }
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(TAG,"Received broadcast!")
            if (intent.action == "com.reactwallet.MESSAGE") {
                val message = intent.getStringExtra(MESSAGE) as String
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun registerReceiver() {
        val intentFilter = IntentFilter("com.reactwallet.MESSAGE")
        reactApplicationContext.registerReceiver(broadcastReceiver, intentFilter)
    }

    private fun unregisterReceiver() {
        reactApplicationContext.unregisterReceiver(broadcastReceiver)
    }

    override fun onCatalystInstanceDestroy() {
        super.onCatalystInstanceDestroy()
        if (isServiceConnected) {
            context.unbindService(serviceConnection)
            isServiceConnected = false
            unbindService()
        }
    }

    private fun timeNow() : String {
        val sdf = SimpleDateFormat("hh:mm:ss")
        val currentTime = Date()
        return sdf.format(currentTime)
    }

    /*
        @ReactMethods
     */

    @ReactMethod
    fun bindService(promise: Promise) {
        Log.d(TAG, "Binding Reactwallet!")
        val intent = Intent(context, MessageService::class.java)
        val bind = context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        registerReceiver()
        promise.resolve(bind)
    }

    @ReactMethod
    fun unbindService() {
        if (isServiceConnected) {
            context.unbindService(serviceConnection)
            isServiceConnected = false
        }
        unregisterReceiver()
    }

    fun sendMessage(message: Bundle) {
        Log.d(MessageService.TAG, "ReactCommClient: Sending message:")
        for (key in message.keySet()) {
            val value = message.get(key)
            Log.d(MessageService.TAG, "$key <- $value")
        }
        messageCallback?.onMessageReceived(message)
    }

    @ReactMethod
    fun sendMessage(message: Bundle, promise: Promise) {
        Log.d(TAG, "Sending new message at ${timeNow()}")
        if (!isServiceConnected) {
            Log.e(TAG,"Service is not connected")
            promise.reject(Exception("Service is not connected"))
            return
        }

        try {
            val messageServiceCallback: IMessegeServiceCallback = object : IMessegeServiceCallback.Stub() {
                override fun onMessageReceived(message: Bundle?) {
                    Log.d(TAG, "Received response at ${timeNow()}")
                    promise.resolve(message)
                }
            }
            messageService?.registerCallback(messageServiceCallback)
            //messageService?.sendMessage(message)
            messageServiceCallback?.onMessageReceived(message)
        } catch (e: Exception) {
            Log.e(TAG,"Could not convert message to Bundle: ${e.message}")
            promise.reject(e)
        }
    }

    @ReactMethod
    fun sayHello(promise: Promise) {
        promise.resolve("Hello back to ya!")
    }
/*
 AndroidService
 */
    @ReactMethod
    fun sendAndroidMessage(messageType: MessageType, message: ReadableMap, promise: Promise) {
        if (!isServiceConnected) {
            Log.e(TAG,"Service is not connected")
            promise.reject(Exception("Service is not connected"))
            return
        }

        when (messageType) {
            MessageType.SEND_MESSAGE -> {
                Arguments.toBundle(message)?.let {
                    sendMessage(it, promise)
                }
            }
            MessageType.RESET_KEYS -> {
                resetKeys(promise)
            }
            MessageType.PING -> {
                ping(promise)
            }
            MessageType.PAUSE -> {
                pause(promise)
            }
            MessageType.RESUME -> {
                resume(promise)
            }
            MessageType.IS_CONNECTED -> {
                isConnected(promise)
            }
            MessageType.DISCONNECT -> {
                disconnect(promise)
            }
            else -> {
                promise.reject(Exception("Invalid message type: $messageType"))
            }
        }
    }

    private fun pause(promise: Promise) {
        Log.d(CommunicationClient.TAG, "Pausing")
        promise.resolve(unbindService())
    }

    private fun resume(promise: Promise) {
        Log.d(CommunicationClient.TAG, "Resuming")
        if (!isServiceConnected) {
            bindService(promise)
        }
    }

    private fun disconnect(promise: Promise) {
        Log.d(CommunicationClient.TAG, "Disconnecting")
        promise.resolve(unbindService())
    }

    private fun isConnected(promise: Promise) {
        Log.d(CommunicationClient.TAG, "isConnected $isServiceConnected")
        promise.resolve(isServiceConnected)
    }

    private fun ping(promise: Promise) {
        Log.d(CommunicationClient.TAG, "Ping")
        try {
            val message = Bundle().apply {
                val bundle = Bundle().apply {
                    putString(CommunicationLayer.MESSAGE_TYPE, MessageType.PING.name)
                }
                putBundle(CommunicationLayer.MESSAGE, bundle)
            }

            val messageServiceCallback: IMessegeServiceCallback = object : IMessegeServiceCallback.Stub() {
                override fun onMessageReceived(message: Bundle?) {
                    Log.d(TAG, "Received ping response")
                    promise.resolve(message)
                }
            }

            messageService?.registerCallback(messageServiceCallback)
            //messageService?.sendMessage(message)
            messageServiceCallback?.onMessageReceived(message)
        } catch (e: Exception) {
            Log.e(TAG,"Could not convert message to Bundle: ${e.message}")
            promise.reject(e)
        }
    }

    private fun getKeyInfo(promise: Promise) {
        Log.d(CommunicationClient.TAG, "Getting key info...")
        promise.resolve(null)
    }

    private fun resetKeys(promise: Promise) {
        Log.d(CommunicationClient.TAG, "Resetting keys...")
        try {
            val message = Bundle().apply {
                val bundle = Bundle().apply {
                    putString(CommunicationLayer.MESSAGE_TYPE, MessageType.RESET_KEYS.name)
                }
                putBundle(CommunicationLayer.MESSAGE, bundle)
            }

            val messageServiceCallback: IMessegeServiceCallback = object : IMessegeServiceCallback.Stub() {
                override fun onMessageReceived(message: Bundle?) {
                    Log.d(TAG, "Received resetKeys response")
                    promise.resolve(message)
                }
            }

            messageService?.registerCallback(messageServiceCallback)
            //messageService?.sendMessage(message)
            messageServiceCallback?.onMessageReceived(message)
        } catch (e: Exception) {
            Log.e(TAG,"Could not convert message to Bundle: ${e.message}")
            promise.reject(e)
        }
    }
}
