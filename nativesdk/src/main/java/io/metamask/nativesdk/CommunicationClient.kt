package io.metamask.nativesdk

import android.content.*
import android.os.IBinder
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.modules.core.DeviceEventManagerModule.RCTDeviceEventEmitter
import org.json.JSONObject

class CommunicationClient(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    private val reactAppContext = reactContext

    override fun getName() = "CommunicationClient"

    private var isServiceConnected = false
    private var messageService: IMessegeService? = null
    var messageCallback: IMessegeServiceCallback? = null
    
    private val keyExchange = KeyExchange.getInstance()
    private val connectionStatusManager: ConnectionStatusManager = ConnectionStatusManager.getInstance()

    private var didPerformTearDown = false

    private val serviceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Logger.log("CommunicationClient:: Service connected $name")
            messageService = IMessegeService.Stub.asInterface(service)
            isServiceConnected = true
            messageService?.registerCallback(messageCallback)
            connectionStatusManager.onMetaMaskConnect()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            if (name.toString().contains("io.metamask.nativesdk.MessageService")) {
                Logger.error("CommunicationClient:: Dapp disconnected $name")
                trackEvent(Event.SDK_DISCONNECTED)
            } else {
                Logger.error("CommunicationClient:: Metamask disconnected $name")
                messageService = null
                isServiceConnected = false
                connectionStatusManager.onMetaMaskDisconnect()
            }
        }
    }

    private fun trackEvent(event: Event) {
        val params: MutableMap<String, String> = mutableMapOf(
            "id" to SessionManager.getInstance().sessionId
        )

        Analytics.trackEvent(event, params)
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {

            val event = intent.getStringExtra("event")
            val data = intent.getStringExtra("data")

            if (event == EventType.BIND.value) {
                Logger.log("CommunicationClient:: Request to bind service")
                bindService()
            } else if (event != null && data != null) {
                broadcastToMetaMask(event, data)
            }
        }
    }

    private fun broadcastToMessageService(message: String) {
        val intent = Intent("local.service")
        intent.putExtra(EventType.MESSAGE.value, message)
        reactAppContext.sendBroadcast(intent)
    }

    fun broadcastToMetaMask(event: String, data: Any) {
        Logger.log("CommunicationClient:: dapp -> metamask $data")
        reactAppContext.getJSModule(
            RCTDeviceEventEmitter::class.java
        )
            .emit(event, data)
    }

    private fun registerReceiver() {
        Logger.log("CommunicationClient:: registerReceiver")
        val intentFilter = IntentFilter("local.client")
        reactAppContext.registerReceiver(broadcastReceiver, intentFilter)
        connectionStatusManager.onMetaMaskBroadcastRegistered()
    }

    private fun unregisterReceiver() {
        Logger.log("CommunicationClient:: unregisterReceiver")
        reactAppContext.unregisterReceiver(broadcastReceiver)
        connectionStatusManager.onMetaMaskBroadcastUnregistered()
    }

    override fun invalidate() {
        super.invalidate()
        Logger.log("CommunicationClient:: invalidate - app terminated")

        if (!didPerformTearDown) {
            destroyActiveConnections()
        }
    }

    override fun onCatalystInstanceDestroy() {
        super.onCatalystInstanceDestroy()
        Logger.log("CommunicationClient:: onCatalystInstanceDestroy - app terminating")

        if (!didPerformTearDown) {
            destroyActiveConnections()
        }
    }

    private fun destroyActiveConnections() {
        unbindService()
        unregisterReceiver()
        connectionStatusManager.onMetaMaskDisconnect()
        didPerformTearDown = true
    }

    /*
        @ReactMethods
     */

    @ReactMethod
    fun bindService(promise: Promise) {
        Logger.log("CommunicationClient:: MetaMask binding")

        if(isServiceConnected) {
            promise.resolve(true)
            return
        }
        promise.resolve(bindService())
    }

    private fun bindService(): Boolean {
        registerReceiver()

        Logger.log("CommunicationClient:: Binding native module")
        val intent = Intent(reactAppContext, MessageService::class.java)
        return reactAppContext.bindService(
            intent,
            serviceConnection,
            Context.BIND_IMPORTANT)

        // Metamask ready, start sending messages
        connectionStatusManager.onMetaMaskReady()
    }

    @ReactMethod
    fun unbindService(promise: Promise) {
        unbindService()
        promise.resolve(null)
    }

    private fun unbindService() {
        Logger.log("CommunicationClient:: unbindService")
        reactAppContext.unbindService(serviceConnection)
    }

    @ReactMethod
    fun sendMessage(message: String, promise: Promise) {

        if (!isServiceConnected) {
            Logger.log("CommunicationClient::sendMessage rx from wallet - service not yet connected: $message")
            promise.resolve(null)
            return
        }

        if (!keyExchange.keysExchanged()) {
            Logger.log("CommunicationClient::sendMessage Keys not exchanged")
            promise.resolve(null)
            return
        }

        promise.resolve(null)

        val json = JSONObject(message)
        val type = json.optString("type")

        if (type == "ready") {
            val dataJson = json.optJSONObject("data")
            val id = dataJson.optString("id")
            val sessionId = SessionManager.getInstance().sessionId

            if (id != sessionId) {
                return
            }
        }

        Logger.log("CommunicationClient::sendMessage wallet -> dapp $message")

        val encrypted = keyExchange.encrypt(message)
        broadcastToMessageService(encrypted)
    }

    @ReactMethod
    fun disconnect(promise: Promise) {
        Logger.log("CommunicationClient:: Disconnecting native module")
        promise.resolve(unbindService())
    }

    @ReactMethod
    fun isConnected(promise: Promise) {
        Logger.log("isConnected: $isServiceConnected")
        promise.resolve(isServiceConnected)
    }
}