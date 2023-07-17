package com.reactwallet

import android.content.*
import android.os.IBinder
import android.util.Log
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule.RCTDeviceEventEmitter
import io.metamask.nativesdk.IMessegeService
import io.metamask.nativesdk.IMessegeServiceCallback

class CommunicationClient(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    private val reactAppContext = reactContext

    override fun getName() = "CommunicationClient"

    companion object {
        const val TAG = "NATIVE_SDK"
    }

    private var isServiceConnected = false
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

            val event = intent.getStringExtra("event")
            val data = intent.getStringExtra("data")

            if (event != null && data != null) {
                broadcastToMetaMask(event, data)
            }
        }
    }

    private fun broadcastToMessageService(message: String) {
        Log.d(MessageService.TAG, "CommunicationClient: Sending message $message to MessageService")
        val intent = Intent("local.service")
        intent.putExtra(EventType.MESSAGE.value, message)
        reactAppContext.sendBroadcast(intent)
    }

    fun broadcastToMetaMask(event: String, data: Any) {
        Log.d(MessageService.TAG, "CommunicationClient: Sending [$event] event to communication layer")
        reactAppContext.getJSModule(
            RCTDeviceEventEmitter::class.java
        )
            .emit(event, data)
    }

    private fun registerReceiver() {
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
        Log.d(TAG, "CommunicationClient: Sending message $message")
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
