package io.metamask.nativesdk

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class KeyExchangeMessageType {
    @SerialName("none")
    NONE,
    @SerialName("key_handshake_start")
    KEY_HANDSHAKE_START,
    @SerialName("key_handshake_check")
    KEY_HANDSHAKE_CHECK,
    @SerialName("key_exchange_SYN")
    KEY_HANDSHAKE_SYN,
    @SerialName("key_exchange_SYNACK")
    KEY_HANDSHAKE_SYNACK,
    @SerialName("key_exchange_ACK")
    KEY_HANDSHAKE_ACK
}