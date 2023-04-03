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


    companion object {
        const val TAG = "MM_ANDROID_SDK"
    }

    private val serviceIntent = Intent().apply {
        component = ComponentName("io.metamask", "io.metamask.MessengerService")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}