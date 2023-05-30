package com.reactwallet

enum class EventType(val value: String) {
    KEY_INFO("key_info"),
    SERVICE_STATUS("service_status"),
    KEYS_EXCHANGED("keys_exchanged"),
    JOIN_CHANNEL("join_channel"),
    CHANNEL_CREATED("channel_created"),
    CLIENTS_CONNECTED("clients_connected"),
    CLIENTS_DISCONNECTED("clients_disconnected"),
    CLIENTS_WAITING("clients_waiting"),
    CLIENTS_READY("clients_ready"),
    SOCKET_DISCONNECTED("socket_disconnected"),
    CONNECTION_STATUS("connection_status"),
    MESSAGE("message"),
    TERMINATE("terminate"),
}