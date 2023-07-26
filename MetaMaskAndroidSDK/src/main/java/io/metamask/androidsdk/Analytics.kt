package io.metamask.androidsdk

import android.util.Log

enum class Event(val value: String) {
    CONNECTIONREQUEST("sdk_connect_request_started"),
    CONNECTED("sdk_connection_established"),
    DISCONNECTED("sdk_disconnected")
}

interface Tracker {
    var enableDebug: Boolean
    fun trackEvent(event: Event, params: MutableMap<String, String>)
}

class Endpoints {
    companion object {
        const val BASE_URL = "https://metamask-sdk-socket.metafi.codefi.network"
        const val ANALYTICS = "$BASE_URL/debug"
    }
}

internal class Analytics(override var enableDebug: Boolean = true) : Tracker {

    private val httpClient: HttpClient = HttpClient()

    override fun trackEvent(event: Event, params: MutableMap<String, String>) {
        if (!enableDebug) { return }

        Logger.log("Analytics: ${event.value}")

        params["event"] = event.value
        httpClient.newCall(Endpoints.ANALYTICS, params)
    }
}