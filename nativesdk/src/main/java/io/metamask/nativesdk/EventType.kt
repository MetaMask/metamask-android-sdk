package io.metamask.nativesdk

enum class EventType(val value: String) {
    KEYS_EXCHANGED("keys_exchanged"),
    CLIENTS_CONNECTED("clients_connected"),
    CLIENTS_DISCONNECTED("clients_disconnected"),
    MESSAGE("message"),
    TERMINATE("terminate"),
    BIND("bind")
}