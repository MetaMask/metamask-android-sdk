package com.reactwallet

enum class KeyExchangeMessageType {
    none,
    key_handshake_start,
    key_handshake_check,
    key_exchange_SYN,
    key_exchange_SYNACK,
    key_exchange_ACK
}