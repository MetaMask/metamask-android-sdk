package io.metamask.androidsdk

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder

class TestApplication : Application() {
    private val mockService = MockIMessegeService()

    override fun bindService(service: Intent, conn: ServiceConnection, flags: Int): Boolean {
        // Simulate successful service binding
        conn.onServiceConnected(ComponentName("io.metamask", "io.metamask.nativesdk.MessageService"), mockService as IBinder)
        return true
    }

    override fun unbindService(conn: ServiceConnection) {
        // Simulate service unbinding
        conn.onServiceDisconnected(ComponentName("io.metamask", "io.metamask.nativesdk.MessageService"))
    }

    fun getMockService(): MockIMessegeService {
        return mockService
    }
}