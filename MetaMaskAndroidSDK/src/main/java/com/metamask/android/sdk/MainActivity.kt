package com.metamask.android.sdk

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.util.Log

class MainActivity : AppCompatActivity() {
    var isBound = false

    lateinit var messengerService: Messenger

    // client messenger
    private lateinit var receiveMessenger: Messenger

    private var messageHandler = MessageHandler()

    companion object {
        const val TAG = "MM_ANDROID_SDK"
    }

    private val serviceIntent = Intent().apply {
        component = ComponentName("io.metamask", "io.metamask.MessengerService")
    }

    private val serviceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            messengerService = Messenger(service)
            receiveMessenger = Messenger(messageHandler)
            messageHandler.serviceMessenger = messengerService
            messageHandler.receiveMessenger =  receiveMessenger
            isBound = true
            Log.d(TAG, "ServiceConnection connected")
            startExchange()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "ServiceConnection disconnected")
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.d(TAG, "Main activity created")
        bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE)
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "Activity started")
    }

    override fun onDestroy() {
        Log.d(TAG, "Main activity destroyed")
        unbindService(serviceConnection)
        super.onDestroy()
    }

    private fun startExchange() {
        if (!isBound) {
            Log.d(TAG, "Service is not bound")
            return
        }
        Log.d(TAG, "SDK initiating communication")
        val message = Message.obtain(null, MessageHandler.CONNECTION)
        message.replyTo = receiveMessenger

        try {
            messengerService.send(message)
        } catch (e: RemoteException) {
            Log.d(TAG, "Encountered remote exception error")
            e.printStackTrace()
        }
    }
}