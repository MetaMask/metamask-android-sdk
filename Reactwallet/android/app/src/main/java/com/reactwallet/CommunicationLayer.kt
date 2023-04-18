package com.reactwallet

import android.os.Bundle
import android.util.Log
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableMap
import com.reactwallet.MessageType.*
import io.metamask.IMessegeService
import io.metamask.IMessegeServiceCallback

class CommunicationLayer(private val messageService: IMessegeService?) {
    companion object {
        const val MESSAGE = "MESSAGE"
        const val MESSAGE_TYPE = "MESSAGE_TYPE"
    }

    @ReactMethod
    fun sendAndroidMessage(messageType: MessageType, message: ReadableMap, promise: Promise) {
        when (messageType) {
            SEND_MESSAGE -> {
                Arguments.toBundle(message)?.let {
                    sendMessage(it, promise)
                }
            }
            RESET_KEYS -> {
                resetKeys(promise)
            }
            PING -> {
                ping(promise)
            }
            PAUSE -> {
                pause(promise)
            }
            RESUME -> {
                resume(promise)
            }
            IS_CONNECTED -> {
                isConnected(promise)
            }
            DISCONNECT -> {
                disconnect(promise)
            }
            else -> {
                promise.reject(Exception("Invalid message type: $messageType"))
            }
        }
    }

    var isKeysExchanged: (() -> Boolean)? = null
    var onPause: (() -> Unit)? = null
    var onResume: ((Promise) -> Unit)? = null
    var onDisconnect: (() -> Unit)? = null
    var isConnected: (() -> Boolean)? = null

    private fun pause(promise: Promise) {
        Log.d(CommunicationClient.TAG, "Pausing")
        onPause?.let { pause ->
            promise.resolve(pause())
        }
    }

    private fun resume(promise: Promise) {
        Log.d(CommunicationClient.TAG, "Resuming")
        onResume?.let { resume ->
            promise.resolve(resume)
        }
    }

    private fun disconnect(promise: Promise) {
        Log.d(CommunicationClient.TAG, "Disconnecting")
        onDisconnect?.let { disconnect ->
            promise.resolve(disconnect())
        }
    }

    private fun isConnected(promise: Promise) {
        Log.d(CommunicationClient.TAG, "isConnected $isConnected")
        isConnected?.let { connected ->
            promise.resolve(connected())
        }
    }

    private fun ping(promise: Promise) {
        Log.d(CommunicationClient.TAG, "Ping")
        try {
            val bundle = Bundle().apply {
                putString(MESSAGE_TYPE, MessageType.PING.name)
            }
            val message = Bundle().apply {
                putBundle(MESSAGE, bundle)
            }
            messageService?.sendMessage(message)
            val messageServiceCallback: IMessegeServiceCallback = object : IMessegeServiceCallback.Stub() {
                override fun onMessageReceived(message: Bundle?) {
                    Log.d(CommunicationClient.TAG, "Received ping response")
                    promise.resolve(message)
                }
            }
            messageService?.registerCallback(messageServiceCallback)
        } catch (e: Exception) {
            Log.e(CommunicationClient.TAG,"Could not convert message to Bundle: ${e.message}")
            promise.reject(e)
        }
    }

    private fun getKeyInfo(promise: Promise) {
        Log.d(CommunicationClient.TAG, "Getting key info...")
    }

    private fun resetKeys(promise: Promise) {
        Log.d(CommunicationClient.TAG, "Resetting keys...")
        try {
            val bundle = Bundle().apply {
                putString(MESSAGE_TYPE, MessageType.RESET_KEYS.name)
            }
            val message = Bundle().apply {
                putBundle(MESSAGE, bundle)
            }
            messageService?.sendMessage(message)
            val messageServiceCallback: IMessegeServiceCallback = object : IMessegeServiceCallback.Stub() {
                override fun onMessageReceived(message: Bundle?) {
                    Log.d(CommunicationClient.TAG, "Received resetKeys response")
                    promise.resolve(message)
                }
            }
            messageService?.registerCallback(messageServiceCallback)
        } catch (e: Exception) {
            Log.e(CommunicationClient.TAG,"Could not convert message to Bundle: ${e.message}")
            promise.reject(e)
        }
    }

    private fun sendMessage(message: Bundle, promise: Promise) {
        Log.d(CommunicationClient.TAG, "Sending new message...")
        try {
            messageService?.sendMessage(message)
            val messageServiceCallback: IMessegeServiceCallback = object : IMessegeServiceCallback.Stub() {
                override fun onMessageReceived(message: Bundle?) {
                    Log.d(CommunicationClient.TAG, "Received response}")
                    promise.resolve(message)
                }
            }
            messageService?.registerCallback(messageServiceCallback)
        } catch (e: Exception) {
            Log.e(CommunicationClient.TAG,"Could not convert message to Bundle: ${e.message}")
            promise.reject(e)
        }
    }
}