package com.reactwallet

import android.app.Activity
import android.app.Notification
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.FrameLayout

class MetaMaskPartialOverlayService : Service() {

    //private lateinit var overlayView: FrameLayout

    override fun onCreate() {
        super.onCreate()

//        val layoutParams = WindowManager.LayoutParams(
//            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
//            WindowManager.LayoutParams.FLAG_BLUR_BEHIND,
//            PixelFormat.OPAQUE
//        )

        //layoutParams.alpha = 1f // Set alpha value to 50%

//        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
//        val displayMetrics = DisplayMetrics()
//        windowManager.defaultDisplay.getMetrics(displayMetrics)
//        val width = (displayMetrics.widthPixels * 0.5f).toInt()
//        val height = (displayMetrics.heightPixels * 0.5f).toInt()
//
//        overlayView = FrameLayout(this).apply {
//            layoutParams = ViewGroup.LayoutParams(width, height)
//            // Customize the smallerWindow view as needed
//            setBackgroundColor(Color.RED)
//        }
//
//        val contentFrameLayout: FrameLayout? = (applicationContext as? Activity)?.findViewById(android.R.id.content)
//        contentFrameLayout?.addView(overlayView)
        val params = WindowManager.LayoutParams().apply {
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

            // Set the flags to make the window non-focusable and translucent
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

            // Set the desired position and size of the window
            gravity = Gravity.BOTTOM
            val displayMetrics = DisplayMetrics()
            width = (displayMetrics.widthPixels * 0.75f).toInt()
            height = (displayMetrics.heightPixels * 0.75f).toInt()
        }

        val containerView = FrameLayout(applicationContext)
        containerView.layoutParams = params

        val intent = Intent(applicationContext, MainActivity::class.java)

        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager.addView(containerView, params)
        containerView.context.startActivity(intent)
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