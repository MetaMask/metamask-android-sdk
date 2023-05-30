package com.reactwallet

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.WindowManager

class OverlayService : Service() {

    private lateinit var overlayView: OverlayView

    override fun onCreate() {
        super.onCreate()

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_BLUR_BEHIND,
            PixelFormat.OPAQUE
        )

        layoutParams.alpha = 1f // Set alpha value to 50%

        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val width = (displayMetrics.widthPixels * 0.3f).toInt()
        val height = (displayMetrics.heightPixels * 0.3f).toInt()
        layoutParams?.width = width
        layoutParams?.height = height

        Log.d("MM_MOBILE","About to layout inflate")
        overlayView = OverlayView(this)
        Log.d("MM_MOBILE","Done layout inflate")

        windowManager.addView(overlayView, layoutParams)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, Notification())
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()

        // Your code here
    }
}