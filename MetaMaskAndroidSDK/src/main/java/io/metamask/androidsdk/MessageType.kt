package io.metamask.androidsdk

enum class MessageType(val value: String) {
    ID("id"),
    TYPE("type"),
    DATA("data"),
    ERROR("error"),
    READY("ready"),
    KEYS_EXCHANGED("keys_exchanged"),
    TERMINATE("terminate"),
}