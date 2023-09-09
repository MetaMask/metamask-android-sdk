package io.metamask.nativesdk

enum class Event(val value: String) {
    CONNECTIONREQUEST("sdk_connect_request_started"),
    CONNECTED("sdk_connection_established"),
    DISCONNECTED("sdk_disconnected")
}