package io.metamask.nativesdk

import android.content.*
import android.os.IBinder
import android.util.Log
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.modules.core.DeviceEventManagerModule.RCTDeviceEventEmitter

class CommunicationClient(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    private val reactAppContext = reactContext

    override fun getName() = "CommunicationClient"

    private var isServiceConnected = false
    private var messageService: IMessegeService? = null
    var messageCallback: IMessegeServiceCallback? = null
    val connectionStatusManager: ConnectionStatusManager = ConnectionStatusManager.getInstance()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            messageService = IMessegeService.Stub.asInterface(service)
            isServiceConnected = true
            messageService?.registerCallback(messageCallback)
            Log.d(TAG,"CommunicationClient: Service connected $name")
            connectionStatusManager.onConnect()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            messageService = null
            isServiceConnected = false
            Log.e(TAG,"CommunicationClient: Service disconnected $name")
            connectionStatusManager.onDisconnect()
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
        Logger.log("CommunicationClient: Sending message to MessageService - $message")
        val intent = Intent("local.service")
        intent.putExtra(EventType.MESSAGE.value, message)
        reactAppContext.sendBroadcast(intent)
    }

    fun broadcastToMetaMask(event: String, data: Any) {
        Logger.log("CommunicationClient: Sending event to metamask - [$event]")
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
        Logger.log("CommunicationClient: Unregistering broadcast receiver")
        reactAppContext.unregisterReceiver(broadcastReceiver)
    }

    override fun invalidate() {
        super.invalidate()
        Logger.log("CommunicationClient: invalidated - app about to be terminated")
        unbindService()
    }

    /*
        @ReactMethods
     */

    @ReactMethod
    fun bindService(promise: Promise) {
        Logger.log("CommunicationClient: bindService called from react")
        if (isServiceConnected) {
            Logger.log("CommunicationClient: service already bound")
            promise.resolve(true)
            return
        }
        val result = bindService()
        promise.resolve(result)
    }

    private fun bindService(): Boolean {
        Logger.log("CommunicationClient: Binding native module")
        val intent = Intent(reactAppContext, MessageService::class.java)
        val result = reactAppContext.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        registerReceiver()
        return result
    }

    @ReactMethod
    fun unbindService(promise: Promise) {
        if (!isServiceConnected) {
            promise.resolve(null)
            return
        }
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
        Logger.log("CommunicationClient: Got message from metamask - $message")
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
        Logger.log("CommunicationClient: Disconnecting native module")
        promise.resolve(unbindService())
    }

    @ReactMethod
    fun isConnected(promise: Promise) {
        Logger.log("isConnected: $isServiceConnected")
        promise.resolve(isServiceConnected)
    }
}