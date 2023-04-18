package com.metamask.android.sdk

data class KeyExchangeMessage(
    val step: String,
    val publicKey: String?
    )

class KeyExchange(crypto: Crypto = Ecies()) {
    companion object {
        const val STEP = "KEY_EXCHANGE_STEP"
        const val PUBLIC_KEY = "PUBLIC_KEY"

        const val KEY_EXCHANGE_START = "key_exchange_start"
        const val KEY_EXCHANGE_SYN = "key_exchange_syn"
        const val KEY_EXCHANGE_SYNACK = "key_exchange_synack"
        const val KEY_EXCHANGE_ACK = "key_exchange_ack"
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

        return when(current.step) {
            KEY_EXCHANGE_START -> KeyExchangeMessage(KEY_EXCHANGE_SYN, publicKey)
            KEY_EXCHANGE_SYN -> KeyExchangeMessage(KEY_EXCHANGE_SYNACK, publicKey)
            KEY_EXCHANGE_SYNACK -> KeyExchangeMessage(KEY_EXCHANGE_ACK, publicKey)
            KEY_EXCHANGE_ACK -> null
            else -> null
        }
    }
}