package io.metamask.nativesdk

interface Tracker {
    var enableDebug: Boolean
    fun trackEvent(event: Event, params: MutableMap<String, String>)
}

class Analytics(override var enableDebug: Boolean = true) : Tracker {

    companion object {
        const val TAG = "MM_ANDROID_SDK"
    }

    private val httpClient: HttpClient = HttpClient()

    override fun trackEvent(event: Event, params: MutableMap<String, String>) {
        if (!enableDebug) { return }

        Logger.log("Analytics: ${event.value}")

        params["event"] = event.value
        httpClient.newCall(Endpoints.ANALYTICS, params)
    }
}