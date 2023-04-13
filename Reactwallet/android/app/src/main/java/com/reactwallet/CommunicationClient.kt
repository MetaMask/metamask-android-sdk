package com.reactwallet

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import com.facebook.react.bridge.Promise

import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import io.metamask.IMessegeService
import io.metamask.IMessegeServiceCallback
import java.text.SimpleDateFormat
import java.util.*

class CommunicationClient(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
    private val context = reactContext

    override fun getName() = "CommunicationClient"

    companion object {
        const val TAG = "REACT_WALLET"
    }

    private var isServiceConnected = false
    private var messageService: IMessegeService? = null

    override fun getConstants(): MutableMap<String, Any> {
        return mutableMapOf(
            "SERVICE_PACKAGE_NAME" to "io.metamask",
            "SERVICE_CLASS_NAME" to "io.metamask.MesssageService"
        )
    }

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

    override fun onCatalystInstanceDestroy() {
        super.onCatalystInstanceDestroy()
        if (isServiceConnected) {
            context.unbindService(serviceConnection)
            isServiceConnected = false
        }
    }

    @ReactMethod
    fun bindService(promise: Promise) {
        Log.d(TAG, "Binding Reactwallet!")
        val intent = Intent(context, MessageService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        promise.resolve("Connected!")
    }

    @ReactMethod
    fun unbindService() {
        if (isServiceConnected) {
            context.unbindService(serviceConnection)
            isServiceConnected = false
        }
    }

    @ReactMethod
    fun sayHello(promise: Promise) {
        promise.resolve("Hello back to ya!")
    }

    fun timeNow() : String {
        val sdf = SimpleDateFormat("hh:mm:ss")
        val currentTime = Date()
        val formattedTime = sdf.format(currentTime)
        return formattedTime
    }
}
