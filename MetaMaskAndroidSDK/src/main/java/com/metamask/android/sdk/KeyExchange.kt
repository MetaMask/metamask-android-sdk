package com.metamask.android.sdk

import com.metamask.android.sdk.KeyExchangeMessageType.*

data class KeyExchangeMessage(
    val type: KeyExchangeMessageType,
    val publicKey: String?
    )

class KeyExchange(crypto: Crypto = Ecies()) {
    companion object {
        const val TYPE = "KEY_EXCHANGE_TYPE"
        const val PUBLIC_KEY = "PUBLIC_KEY"
    }

    private val privateKey: String?
    private val publicKey: String?
    private var theirPublickKey: String? = null

    private val encryption: Crypto

    init {
        encryption = crypto
        privateKey = encryption.generatePrivateKey()
        publicKey = encryption.publicKey(privateKey)
    }

    fun encrypt(message: String): String {
        val key: String = theirPublickKey ?: throw NullPointerException("theirPublickKey is null")
        return encryption.encrypt(message, key)
    }

    fun decrypt(message: String): String {
        val key: String = privateKey ?: throw NullPointerException("privateKey is null")
        return encryption.decrypt(message, key)
    }

    fun nextKeyExchangeMessage(current: KeyExchangeMessage): KeyExchangeMessage? {
        theirPublickKey = current.publicKey

        return when(current.type) {
            key_handshake_start -> KeyExchangeMessage(key_exchange_SYN, publicKey)
            key_exchange_SYN -> KeyExchangeMessage(key_exchange_SYNACK, publicKey)
            key_exchange_SYNACK -> KeyExchangeMessage(key_exchange_ACK, publicKey)
            key_exchange_ACK -> null
            else -> null
        }
    }
}