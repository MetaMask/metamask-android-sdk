package com.metamask.android.sdk

enum class MessageType(val value: String) {
    ID("id"),
    TYPE("type"),
    DATA("data"),
    ERROR("error"),
    PAUSE("pause"),
    READY("ready"),
    KEYS_EXCHANGED("keys_exchanged"),
    TERMINATE("terminate"),
    WALLET_INFO("wallet_info")
}