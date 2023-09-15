package io.metamask.nativesdk

enum class Event(val value: String) {
    SDK_CONNECTION_REQUEST_STARTED("sdk_connect_request_started"),
    SDK_CONNECTION_ESTABLISHED("sdk_connection_established"),
    SDK_DISCONNECTED("sdk_disconnected")
}