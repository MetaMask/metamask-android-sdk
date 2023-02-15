package com.metamask.android.sdk

data class MessageInfo (
    val key: String?,
    val value: String?,
    )

data class KeyExchangeMessage(
    val step: String,
    val publicKey: String?
    )

class KeyExchange(crypto: Crypto) {
    companion object {
        const val TAG = "MM_ANDROID_SDK"
        const val KEY_EXCHANGE = "key_exchange"

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

    fun updateTheirPublicKey(key: String) {
        theirPublickKey = key
    }

    fun nextKeyExchangeMessage(current: KeyExchangeMessage): MessageInfo? {
        return when(current.step) {
            KEY_EXCHANGE_START -> MessageInfo(KEY_EXCHANGE, KEY_EXCHANGE_SYN)
            KEY_EXCHANGE_SYN -> MessageInfo(KEY_EXCHANGE, KEY_EXCHANGE_SYNACK)
            KEY_EXCHANGE_SYNACK -> MessageInfo(KEY_EXCHANGE, KEY_EXCHANGE_ACK)
            KEY_EXCHANGE_ACK -> null
            else -> null
        }
    }
}