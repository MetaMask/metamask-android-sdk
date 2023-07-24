package io.metamask.nativesdk

import io.metamask.nativesdk.KeyExchangeMessageType.*

data class KeyExchangeMessage(
    val type: String,
    val publicKey: String?
)

object KeyExchange {
    const val TYPE = "type"
    const val PUBLIC_KEY = "public_key"
    var step: String = KeyExchangeMessageType.none.name
    private val crypto: Crypto = Crypto()

    private lateinit var privateKey: String
    lateinit var publicKey: String
    private var theirPublicKey: String? = null
    var keysExchanged = false

    init {
        resetKeys()
    }

    fun resetKeys() {
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

    fun complete() {
        keysExchanged = true
    }

    fun nextKeyExchangeMessage(current: KeyExchangeMessage): KeyExchangeMessage? {
        current.publicKey?.let {
            theirPublicKey = it
        }

        step = current.type

        val nextStep = when(current.type) {
            key_handshake_start.name -> KeyExchangeMessage(key_exchange_SYN.name, publicKey)
            key_exchange_SYN.name -> KeyExchangeMessage(key_exchange_SYNACK.name, publicKey)
            key_exchange_SYNACK.name -> KeyExchangeMessage(key_exchange_ACK.name, publicKey)
            key_exchange_ACK.name -> null
            else -> null
        }

        step = nextStep?.type ?: KeyExchangeMessageType.none.name
        return nextStep
    }
}