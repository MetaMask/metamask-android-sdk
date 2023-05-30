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
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.logging.Logger

class CommunicationClient(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    private val context = reactContext

    override fun getName() = "CommunicationClient"

    companion object {
        const val TAG = "MM_MOBILE"
        const val MESSAGE_TYPE = "message_type"
    }

    private var isServiceConnected = false
    private var messageService: IMessegeService? = null
    var messageCallback: IMessegeServiceCallback? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            messageService = IMessegeService.Stub.asInterface(service)
            isServiceConnected = true
            messageService?.registerCallback(messageCallback)
            Log.d(TAG,"ReactCommClient: Service connected $name")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            messageService = null
            isServiceConnected = false
            Log.e(TAG,"ReactCommClient: Service disconnected $name")
        }
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "local.client" && intent.getStringExtra(EventType.MESSAGE.value)?.isNotEmpty() == true) {
                val message = intent.getStringExtra(EventType.MESSAGE.value)
                Log.d(TAG,"Got broadcast message: $message")

                val messageJson = JSONObject(message)
                val data = messageJson.optString("data")
                val dataJson = JSONObject(data)
                val id = dataJson.optString("id")

                if (id.isNotEmpty()) {
                    sendAccountsInfo(id)
                }

                sendMetaMaskAccountsChangedEvent()
            } else if (intent.action == "local.client" && intent.getStringExtra(EventType.KEYS_EXCHANGED.value)?.isNotEmpty() == true) {
                val message = intent.getStringExtra(EventType.KEYS_EXCHANGED.value) as String
                sendWalletInfo()
                broadcastToMetaMask(message)
            }
        }
    }

    fun broadcastToMessageService(message: String) {
        Log.d(MessageService.TAG, "CommClient: Broadcasting message to MessageService...")
        val intent = Intent("local.service")
        intent.putExtra(EventType.MESSAGE.value, message)
        context.sendBroadcast(intent)
    }

    fun broadcastToMetaMask(message: String) {
        Log.d(MessageService.TAG, "CommClient: Broadcasting message to MetaMask...")
        val intent = Intent("io.metamask")
        intent.putExtra(EventType.MESSAGE.value, message)
        context.sendBroadcast(intent)
    }

    private fun sendWalletInfo() {
        val walletInfoJson = JSONObject().apply {
            val data = JSONObject().apply {
                put("type", "metamask wallet")
                put("version", "6.5.2")
            }
            put("type", "wallet_info")
            put("data", data.toString())
        }

        val walletInfo = walletInfoJson.toString()
        Log.d(TAG,"Sending wallet info: $walletInfo")

        val walletEncrypted = KeyExchange.encrypt(walletInfo)
        broadcastToMessageService(walletEncrypted)
    }

    private fun sendAccountsInfo(id: String) {
        val accountInfoJson = JSONObject().apply {
            val data = JSONObject().apply {
                val accounts = JSONObject().apply {
                    put("accounts", listOf("12hfhw88374h3j3dhd873733"))
                    put("chainId", "0x1")
                }
                put("result", accounts.toString())
                put("id", id)
            }
            put("data", data.toString())
        }

        val accountsInfo = accountInfoJson.toString()

        Log.d(TAG, "Sending accounts info: $accountsInfo")

        val accountInfoEncrypted = KeyExchange.encrypt(accountsInfo)
        broadcastToMessageService(accountInfoEncrypted)
    }

    private fun sendAccountsError(id: String) {
        val errorJson = JSONObject().apply {
            val data = JSONObject().apply {
                val error = JSONObject().apply {
                    put("message", "User rejected request")
                    put("code", 1200)
                }
                put("error", error.toString())
                put("id", id)
            }
            put("data", data.toString())
        }

        val errorInfo = errorJson.toString()

        Log.d(TAG, "Sending accounts error: $errorInfo")

        val errorInfoEncrypted = KeyExchange.encrypt(errorInfo)
        broadcastToMessageService(errorInfoEncrypted)
    }

    private fun sendMetaMaskAccountsChangedEvent() {
        val accountInfoJson = JSONObject().apply {
            val data = JSONObject().apply {
                put("params", listOf("12hfhw88374h3j3dhd873733"))
                put("method", "metamask_accountsChanged")
            }
            put("data", data.toString())
        }

        val accountsInfo = accountInfoJson.toString()

        Log.d(TAG, "Sending accounts info event: $accountsInfo")

        val accountInfoEncrypted = KeyExchange.encrypt(accountsInfo)
        broadcastToMessageService(accountInfoEncrypted)
    }

    private fun registerReceiver() {
        Log.d(TAG, "ReactCommClient: Registering broadcast receiver")
        val intentFilter = IntentFilter("local.client")
        context.registerReceiver(broadcastReceiver, intentFilter)
    }

    private fun unregisterReceiver() {
        Log.d(TAG, "ReactCommClient: Deregistering broadcast receiver")
        context.unregisterReceiver(broadcastReceiver)
    }

    override fun onCatalystInstanceDestroy() {
        super.onCatalystInstanceDestroy()
        if (isServiceConnected) {
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

    fun sendMessage(message: String) {
        Log.d(TAG, "ReactCommClient: Sending message: $message")
        val message = Bundle().apply {
            putString("message", message)
        }
        //messageService?.sendMessage(message)
        messageCallback?.onMessageReceived(message)
    }

    @ReactMethod
    fun sendMessage(message: String, promise: Promise) {
        Log.d(TAG, "Sending new message at ${timeNow()}")
        if (!isServiceConnected) {
            Log.e(TAG,"Service is not connected")
            promise.reject(Exception("Service is not connected"))
            return
        }

        try {
            val messageServiceCallback: IMessegeServiceCallback = object : IMessegeServiceCallback.Stub() {
                override fun onMessageReceived(message: Bundle) {
                    val message = message.getString("message") ?: String()
                    Log.d(TAG, "Received response at ${timeNow()}")
                    promise.resolve(message)
                }
            }
            messageService?.registerCallback(messageServiceCallback)
            //messageService?.sendMessage(message)
            val bundle = Bundle().apply {
                putString("message", message)
            }
            messageServiceCallback?.onMessageReceived(bundle)
        } catch (e: Exception) {
            Log.e(TAG,"Could not send message: ${e.message}")
            promise.reject(e)
        }
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
//                Arguments.toBundle(message)?.let {
//                    sendMessage(it, promise)
//                }
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
        Log.d(TAG, "Pausing")
        promise.resolve(unbindService())
    }

    private fun resume(promise: Promise) {
        Log.d(TAG, "Resuming")
        if (!isServiceConnected) {
            bindService(promise)
        }
    }

    private fun disconnect(promise: Promise) {
        Log.d(TAG, "Disconnecting")
        promise.resolve(unbindService())
    }

    private fun isConnected(promise: Promise) {
        Log.d(TAG, "isConnected $isServiceConnected")
        promise.resolve(isServiceConnected)
    }

    private fun ping(promise: Promise) {
        Log.d(TAG, "Ping")
        try {
            val messageJson = JSONObject().apply {
                val details = JSONObject().apply {
                    put(MESSAGE_TYPE, MessageType.PING.name)
                }
                val payload = KeyExchange.encrypt(details.toString())
                put(EventType.MESSAGE.value, payload)
            }

            val messageServiceCallback: IMessegeServiceCallback = object : IMessegeServiceCallback.Stub() {
                override fun onMessageReceived(message: Bundle) {
                    val message = message.getString("message") ?: String()
                    Log.d(TAG, "Received ping response")
                    promise.resolve(message)
                }
            }

            messageService?.registerCallback(messageServiceCallback)
            //messageService?.sendMessage(message)
            val message = Bundle().apply {
                putString("message", messageJson.toString())
            }
            messageServiceCallback?.onMessageReceived(message)
        } catch (e: Exception) {
            Log.e(TAG,"Could not send message: ${e.message}")
            promise.reject(e)
        }
    }

    private fun getKeyInfo(promise: Promise) {
        Log.d(TAG, "Getting key info...")
        promise.resolve(null)
    }

    private fun resetKeys(promise: Promise) {
        Log.d(TAG, "Resetting keys...")
        try {

            val messageJson = JSONObject().apply {
                val details = JSONObject().apply {
                    put(MESSAGE_TYPE, MessageType.RESET_KEYS.name)
                }
                val payload = KeyExchange.encrypt(details.toString())
                put(EventType.MESSAGE.value, payload)
            }

            val messageServiceCallback: IMessegeServiceCallback = object : IMessegeServiceCallback.Stub() {
                override fun onMessageReceived(message: Bundle) {
                    val message = message.getString("message") ?: String()
                    Log.d(TAG, "Received resetKeys response")
                    promise.resolve(message)
                }
            }

            messageService?.registerCallback(messageServiceCallback)
            //messageService?.sendMessage(message)
            val message = Bundle().apply {
                putString("message", messageJson.toString())
            }
            messageServiceCallback?.onMessageReceived(message)
        } catch (e: Exception) {
            Log.e(TAG,"Could not convert message to Bundle: ${e.message}")
            promise.reject(e)
        }
    }
}
