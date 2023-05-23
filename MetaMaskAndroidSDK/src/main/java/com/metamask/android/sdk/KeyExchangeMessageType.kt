package com.metamask.android.sdk

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class KeyExchangeMessageType {
    @SerialName("none")
    none,
    @SerialName("key_handshake_start")
    key_handshake_start,
    @SerialName("key_handshake_check")
    key_handshake_check,
    @SerialName("key_exchange_SYN")
    key_exchange_SYN,
    @SerialName("key_exchange_SYNACK")
    key_exchange_SYNACK,
    @SerialName("key_exchange_ACK")
    key_exchange_ACK
}