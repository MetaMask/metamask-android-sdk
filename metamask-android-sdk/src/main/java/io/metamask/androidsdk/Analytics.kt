


package io.metamask.androidsdk


enum class Event(val value: String) {
    SDK_CONNECTION_REQUEST_STARTED("sdk_connect_request_started"),
    SDK_CONNECTION_ESTABLISHED("sdk_connection_established"),
    SDK_CONNECTION_AUTHORIZED("sdk_connection_authorized"),
    SDK_CONNECTION_REJECTED("sdk_connection_rejected"),
    SDK_CONNECTION_FAILED("sdk_connection_failed"),
    SDK_DISCONNECTED("sdk_disconnected")
}

interface Tracker {
    var enableDebug: Boolean
    fun trackEvent(event: Event, params: MutableMap<String, String>)
}

class Endpoints {
    companion object {
        private const val BASE_URL = "https://metamask-sdk-socket.metafi.codefi.network"
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