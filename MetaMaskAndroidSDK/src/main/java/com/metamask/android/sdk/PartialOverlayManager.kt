package com.metamask.android.sdk

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts

class PartialOverlayManager(context: Context): Service() {
    val appContext = context

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

}

