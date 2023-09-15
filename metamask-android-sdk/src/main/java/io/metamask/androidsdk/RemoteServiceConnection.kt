package io.metamask.androidsdk

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import io.metamask.nativesdk.IMessegeService
import io.metamask.nativesdk.IMessegeServiceCallback
import java.lang.ref.WeakReference
import javax.inject.Provider

class RemoteServiceConnection(
    applicationRepository: ApplicationRepository,
    callback: Provider<RemoteMessageServiceCallback>
    ): ServiceConnection {
    var isServiceConnected = false
        private set

    private var messageService: IMessegeService? = null
    private val appContextRef: WeakReference<Context> = WeakReference(applicationRepository.context)
    private val remoteMessageServiceCallback: RemoteMessageServiceCallback by lazy {
        callback.get()
    }

    companion object {
        const val REMOTE_PACKAGE = "io.metamask"
        const val REMOTE_PACKAGE_SERVICE = "io.metamask.nativesdk.MessageService"
    }

    private val messageServiceCallback: IMessegeServiceCallback = object : IMessegeServiceCallback.Stub() {
        override fun onMessageReceived(bundle: Bundle) {
            remoteMessageServiceCallback.handleRemoteMessage(bundle)
        }
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        // Called when the remote service is connected
        Logger.log("RemoteServiceConnection:: onServiceConnected")
        messageService = IMessegeService.Stub.asInterface(service)
        messageService?.registerCallback(messageServiceCallback)
        isServiceConnected = true
        remoteMessageServiceCallback.initiateKeyExchange()
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        // Called when the remote service is disconnected
        Logger.log("RemoteServiceConnection:: onServiceDisconnected")
        messageService = null
        isServiceConnected = false
    }

    fun sendMessage(bundle: Bundle) {
        messageService?.sendMessage(bundle)
    }

    fun connect() {
        Logger.log("RemoteServiceConnection:: Binding service")
        val serviceIntent = Intent()
            .setComponent(
                ComponentName(
                    REMOTE_PACKAGE,
                    REMOTE_PACKAGE_SERVICE
                )
            )

        if (appContextRef.get() != null) {
            appContextRef.get()?.bindService(
                serviceIntent,
                this,
                Context.BIND_AUTO_CREATE)
        } else {
            Logger.error("App context null!")
        }
    }

    fun disconnect() {
        if (isServiceConnected) {
            Logger.log("RemoteServiceConnection:: Unbinding service")
            appContextRef.get()?.unbindService(this)
            isServiceConnected = false
        }
    }
}