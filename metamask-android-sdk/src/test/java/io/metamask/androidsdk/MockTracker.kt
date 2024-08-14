package io.metamask.androidsdk

class MockTracker(override var enableDebug: Boolean = true) : Tracker {

    var trackedEvent: Event? = null
    var trackedEventParams: MutableMap<String, String>? = null

    override fun trackEvent(event: Event, params: MutableMap<String, String>) {
        if (!enableDebug) { return }

        params["event"] = event.value
        trackedEvent = event
        trackedEventParams = params
    }
}