package io.metamask.nativesdk

import io.metamask.nativesdk.KeyExchangeMessageType.*

data class KeyExchangeMessage(
    val type: String,
    val publicKey: String?
)

class KeyExchange {
    var step: String = NONE.name
    private val crypto: Crypto = Crypto()

    private lateinit var privateKey: String
    lateinit var publicKey: String
    private var theirPublicKey: String? = null
    private var isKeysExchanged = false

    init {
        resetKeys()
    }

    companion object {
        const val TYPE = "type"
        const val PUBLIC_KEY = "public_key"

        private var instance: KeyExchange? = null

        fun getInstance(): KeyExchange {
            if (instance == null) {
                instance = KeyExchange()
            }
            return instance as KeyExchange
        }
    }

    private fun setIsKeysExchanged(newValue: Boolean) {
        synchronized(this) {
            isKeysExchanged = newValue
        }
    }

    fun keysExchanged(): Boolean {
        synchronized(this) {
            return isKeysExchanged
        }
    }

    fun resetKeys() {
        privateKey = crypto.generatePrivateKey()
        privateKey?.let {
            publicKey = crypto.publicKey(it)
        }
        setIsKeysExchanged(false)
        theirPublicKey = null
    }

    fun encrypt(message: String): String {
        val key: String = theirPublicKey ?: throw NullPointerException("theirPublicKey is null")
        return crypto.encrypt(key, message)
    }

    fun decrypt(message: String): String {
        val key: String = privateKey
        return crypto.decrypt(key, message)
    }

    fun complete() {
        setIsKeysExchanged(true)
    }

    fun nextKeyExchangeMessage(current: KeyExchangeMessage): KeyExchangeMessage? {
        current.publicKey?.let {
            theirPublicKey = it
        }

        step = current.type

        val nextStep = when(current.type) {
            KEY_HANDSHAKE_START.name -> KeyExchangeMessage(KEY_HANDSHAKE_SYN.name, publicKey)
            KEY_HANDSHAKE_SYN.name -> KeyExchangeMessage(KEY_HANDSHAKE_SYNACK.name, publicKey)
            KEY_HANDSHAKE_SYNACK.name -> KeyExchangeMessage(KEY_HANDSHAKE_ACK.name, publicKey)
            KEY_HANDSHAKE_ACK.name -> null
            else -> null
        }

        step = nextStep?.type ?: NONE.name
        return nextStep
    }
}