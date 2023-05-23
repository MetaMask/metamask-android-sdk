package com.metamask.android.sdk

enum class MessageType(val value: String) {
    ID("id"),
    TYPE("type"),
    DATA("data"),
    PAUSE("pause"),
    READY("ready"),
    TERMINATE("terminate"),
    WALLET_INFO("wallet_info")
}