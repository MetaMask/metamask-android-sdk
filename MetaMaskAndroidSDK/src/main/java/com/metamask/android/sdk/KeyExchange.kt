package com.metamask.android.sdk

import kotlinx.serialization.Serializable
import com.metamask.android.sdk.KeyExchangeMessageType.*
import kotlinx.serialization.SerialName

data class KeyExchangeMessage(
    val type: String,
    val publicKey: String?
    )

class KeyExchange(private val crypto: Crypto = Crypto()) {
    companion object {
        const val TYPE = "type"
        const val PUBLIC_KEY = "public_key"
    }

    private var privateKey: String? = null
    var publicKey: String? = null
    private var theirPublicKey: String? = null
    var keysExchanged = false

    init {
        generateNewKeys()
    }

    fun generateNewKeys() {
        privateKey = crypto.generatePrivateKey()
        privateKey?.let {
            publicKey = crypto.publicKey(it)
        }
        keysExchanged = false
        theirPublicKey = null
    }

    fun encrypt(message: String): String {
        val key: String = theirPublicKey ?: throw NullPointerException("theirPublicKey is null")
        return crypto.encrypt(key, message)
    }

    fun decrypt(message: String): String {
        val key: String = privateKey ?: throw NullPointerException("privateKey is null")
        return crypto.decrypt(key, message)
    }

    fun nextKeyExchangeMessage(current: KeyExchangeMessage): KeyExchangeMessage? {
        current.publicKey?.let {
            theirPublicKey = it
            keysExchanged = true
        }

        return when(current.type) {
            key_handshake_start.name -> KeyExchangeMessage(key_exchange_SYN.name, publicKey)
            key_exchange_SYN.name -> KeyExchangeMessage(key_exchange_SYNACK.name, publicKey)
            key_exchange_SYNACK.name -> KeyExchangeMessage(key_exchange_ACK.name, publicKey)
            key_exchange_ACK.name -> null
            else -> null
        }
    }
}