package com.reactwallet

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
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
    }

    private var isServiceConnected = false
    private var messageService: IMessegeService? = null
    private lateinit var communicationLayer: CommunicationLayer

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            messageService = IMessegeService.Stub.asInterface(service)
            isServiceConnected = true
            Log.d(TAG,"Service connected $name")

            communicationLayer = CommunicationLayer(messageService)
            communicationLayer.isConnected = { isServiceConnected }
            communicationLayer.onDisconnect = { unbindService() }
            communicationLayer.onPause = { unbindService() }
            communicationLayer.onResume = { promise ->
                if (!isServiceConnected) {
                    bindService(promise)
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            messageService = null
            isServiceConnected = false
            Log.e(TAG,"Service disconnected $name")
        }
    }

    override fun onCatalystInstanceDestroy() {
        super.onCatalystInstanceDestroy()
        if (isServiceConnected) {
            context.unbindService(serviceConnection)
            isServiceConnected = false
        }
    }

    private fun timeNow() : String {
        val sdf = SimpleDateFormat("hh:mm:ss")
        val currentTime = Date()
        val formattedTime = sdf.format(currentTime)
        return formattedTime
    }

    /*
        @ReactMethods
     */

    @ReactMethod
    fun bindService(promise: Promise) {
        Log.d(TAG, "Binding Reactwallet!")
        val intent = Intent(context, MessageService::class.java)
        val bind = context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        promise.resolve(bind)
    }

    @ReactMethod
    fun unbindService() {
        if (isServiceConnected) {
            context.unbindService(serviceConnection)
            isServiceConnected = false
        }
    }

    @ReactMethod
    fun sendAndroidMessage(messageType: MessageType, message: ReadableMap, promise: Promise) {
        if (!isServiceConnected) {
            Log.e(TAG,"Service is not connected")
            promise.reject(Exception("Service is not connected"))
            return
        }
        communicationLayer.sendAndroidMessage(messageType, message, promise)
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
            messageService?.sendMessage(message)
            val messageServiceCallback: IMessegeServiceCallback = object : IMessegeServiceCallback.Stub() {
                override fun onMessageReceived(message: Bundle?) {
                    Log.d(TAG, "Received response at ${timeNow()}")
                    promise.resolve(message)
                }
            }
            messageService?.registerCallback(messageServiceCallback)
        } catch (e: Exception) {
            Log.e(TAG,"Could not convert message to Bundle: ${e.message}")
            promise.reject(e)
        }
    }

    @ReactMethod
    fun sayHello(promise: Promise) {
        promise.resolve("Hello back to ya!")
    }
}
