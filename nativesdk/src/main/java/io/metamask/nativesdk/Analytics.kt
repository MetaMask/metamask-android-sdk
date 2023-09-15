package io.metamask.nativesdk

// Making Analytics static as CommunicationClient cannot be instantiated
class Analytics {
    companion object {
        var enableDebug: Boolean = true
        private val httpClient: HttpClient = HttpClient()

        fun trackEvent(event: Event, params: MutableMap<String, String>) {
            if (!enableDebug) { return }

            Logger.log("Analytics: ${event.value}")

            params["event"] = event.value
            httpClient.newCall(Endpoints.ANALYTICS, params)
        }
    }
}