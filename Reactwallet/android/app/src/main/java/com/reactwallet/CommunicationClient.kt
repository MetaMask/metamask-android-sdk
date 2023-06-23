package com.reactwallet

import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.contentcapture.ContentCaptureSessionId
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule.RCTDeviceEventEmitter
import io.metamask.IMessegeService
import io.metamask.IMessegeServiceCallback
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*


class CommunicationClient(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    private val reactAppContext = reactContext

    override fun getName() = "CommunicationClient"

    companion object {
        const val TAG = "AIDL_NATIVE_MODULE"
        const val MESSAGE_TYPE = "message_type"
        const val SESSION_ID = "session_id"
    }

    private var channelId = ""
    private var isServiceConnected = false
    private var clientsConnected = false
    private var messageService: IMessegeService? = null
    var messageCallback: IMessegeServiceCallback? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            messageService = IMessegeService.Stub.asInterface(service)
            isServiceConnected = true
            messageService?.registerCallback(messageCallback)
            Log.d(TAG,"CommunicationClient: Service connected $name")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            messageService = null
            isServiceConnected = false
            Log.e(TAG,"CommunicationClient: Service disconnected $name")
        }
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val sessionId = intent.getStringExtra(SESSION_ID)
            val event = intent.getStringExtra("event")
            val data = intent.getStringExtra("data")

            if (sessionId != null && !clientsConnected) {
                clientsConnected = true
                channelId = sessionId
                broadcastToMetaMask(EventType.CLIENTS_CONNECTED.value, sessionId)
            }

            if (event != null && data != null) {
                broadcastToMetaMask(event, data)
            } else if (event != null) {
                broadcastToMetaMask(event, sessionId)
            }

            val message = intent.getStringExtra(EventType.MESSAGE.value)

            if (intent.action == "local.client" && message != null) {

                broadcastToMetaMask(EventType.MESSAGE.value, message)
                Log.d(TAG,"CommunicationClient: Got client broadcast message: $message")

                val messageJson = JSONObject(message)
                val id = messageJson.optString("id")

                if (id.isNotEmpty()) {
                    sendAccountsInfo(id)
                }

                sendMetaMaskAccountsChangedEvent()
            } else if (intent.action == "local.client" && intent.getStringExtra(EventType.KEYS_EXCHANGED.value)?.isNotEmpty() == true) {

                sendWalletInfo()
                broadcastToMetaMask(EventType.KEYS_EXCHANGED.value)
            }
        }
    }

    private fun broadcastToMessageService(message: String) {
        Log.d(MessageService.TAG, "CommunicationClient: Broadcasting message $message to MessageService")
        val intent = Intent("local.service")
        intent.putExtra(EventType.MESSAGE.value, message)
        reactAppContext.sendBroadcast(intent)
    }

    fun broadcastToMetaMask(event: String, data: Any? = null) {
        Log.d(MessageService.TAG, "CommunicationClient: Sending event $event to MetaMask wallet")
        reactAppContext.getJSModule(
            RCTDeviceEventEmitter::class.java
        )
            .emit(event, data)
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
        Log.d(TAG,"CommunicationClient: Sending wallet info: $walletInfo")

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

    private fun sendMetaMaskAccountsChangedEvent() {
        val accountInfoJson = JSONObject().apply {
            val data = JSONObject().apply {
                put("params", listOf("12hfhw88374h3j3dhd873733"))
                put("method", "metamask_accountsChanged")
            }
            put("data", data.toString())
        }

        val accountsInfo = accountInfoJson.toString()

        Log.d(TAG, "CommunicationClient: Sending accounts info event")

        val accountInfoEncrypted = KeyExchange.encrypt(accountsInfo)
        broadcastToMessageService(accountInfoEncrypted)
    }

    private fun registerReceiver() {
        Log.d(TAG, "CommunicationClient: Registering broadcast receiver")
        val intentFilter = IntentFilter("local.client")
        reactAppContext.registerReceiver(broadcastReceiver, intentFilter)
    }

    private fun unregisterReceiver() {
        Log.d(TAG, "CommunicationClient: Deregistering broadcast receiver")
        reactAppContext.unregisterReceiver(broadcastReceiver)
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
        val result = bindService()
        promise.resolve(result)


    }

    private fun bindService(): Boolean {
        Log.d(TAG, "CommunicationClient: Binding native module")
        val intent = Intent(reactAppContext, MessageService::class.java)
        val result = reactAppContext.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        registerReceiver()
        return result
    }

    @ReactMethod
    fun unbindService(promise: Promise) {
        unbindService()
        promise.resolve(null)
    }

    private fun unbindService() {
        if (isServiceConnected) {
            reactAppContext.unbindService(serviceConnection)
            isServiceConnected = false
        }
        unregisterReceiver()
    }

    @ReactMethod
    fun sendMessage(message: String, promise: Promise) {
        Log.d(TAG, "CommunicationClient: Sending message $message, ${timeNow()}")
        if (!isServiceConnected) {
            Log.e(TAG,"CommunicationClient: Service is not connected")
            promise.reject(Exception("Service is not connected"))
            return
        }
        promise.resolve(null)

        val encrypted = KeyExchange.encrypt(message)
        broadcastToMessageService(encrypted)
    }

    @ReactMethod
    fun disconnect(promise: Promise) {
        Log.d(TAG, "CommunicationClient: Disconnecting native module")
        promise.resolve(unbindService())
    }

    @ReactMethod
    fun isConnected(promise: Promise) {
        Log.d(TAG, "isConnected: $isServiceConnected")
        promise.resolve(isServiceConnected)
    }
}
