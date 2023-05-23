package com.metamask.android.sdk

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager

class PartialOverlayService : Service() {
    private lateinit var overlayView: OverlayView
    private lateinit var windowManager: WindowManager

    private val layoutParams = WindowManager.LayoutParams().apply {
        // Set the window type to overlay
        type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

        // Set the flags to make the window non-focusable and translucent
        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

        // Set the desired position and size of the window
        gravity = Gravity.TOP or Gravity.START
        x = 100
        y = 100
        width = WindowManager.LayoutParams.WRAP_CONTENT
        height = WindowManager.LayoutParams.WRAP_CONTENT
    }

    override fun onCreate() {
        super.onCreate()
        Logger.log("Creating partial overlay")

        overlayView = OverlayView(this)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val packageName = "com.reactwallet"
        val intent = packageManager.getLaunchIntentForPackage(packageName)

        if (intent != null) {
            // Launch the app within a separate window
            windowManager.addView(overlayView, layoutParams)
            startActivity(intent)
        } else {
            // Fallback for when MetaMask is not installed
            Logger.error("Metamask is not installed")
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Remove the overlay view
        windowManager.removeView(overlayView)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
