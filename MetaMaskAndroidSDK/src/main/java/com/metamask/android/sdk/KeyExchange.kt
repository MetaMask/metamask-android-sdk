package com.metamask.android.sdk

import com.metamask.android.sdk.KeyExchangeMessageType.*

data class KeyExchangeMessage(
    val type: KeyExchangeMessageType,
    val publicKey: String?
    )

class KeyExchange(private val crypto: Crypto = Crypto()) {
    companion object {
        const val TYPE = "KEY_EXCHANGE_TYPE"
        const val PUBLIC_KEY = "PUBLIC_KEY"
    }

    private val privateKey: String?
    private val publicKey: String?
    private var theirPublickKey: String? = null

    init {
        privateKey = crypto.generatePrivateKey()
        publicKey = crypto.publicKey(privateKey)
    }

    fun encrypt(message: String): String {
        val key: String = theirPublickKey ?: throw NullPointerException("theirPublickKey is null")
        return crypto.encrypt(key, message)
    }

    fun decrypt(message: String): String {
        val key: String = privateKey ?: throw NullPointerException("privateKey is null")
        return crypto.decrypt(key, message)
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