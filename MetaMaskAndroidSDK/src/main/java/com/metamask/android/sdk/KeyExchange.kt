package com.metamask.android.sdk

import android.accessibilityservice.GestureDescription.StrokeDescription
import android.os.Bundle
import android.os.Message
import android.util.Log

data class MessageInfo (
    val step: Int,
    val key: String?,
    val value: String?
)

class KeyExchange {
    companion object {
        const val TAG = "MM_ANDROID_SDK"

        const val CONNECTION_STEP = 0
        const val KEY_EXCHANGE_STEP = 1
        const val MESSAGE_STEP = 2

        const val KEY_EXCHANGE = "key_exchange"
        const val MESSAGE = "message"

        const val KEY_EXCHANGE_START = "key_exchange_start"
        const val KEY_EXCHANGE_SYN = "key_exchange_syn"
        const val KEY_EXCHANGE_SYNACK = "key_exchange_synack"
        const val KEY_EXCHANGE_ACK = "key_exchange_ack"
    }

    private val privateKey: String? = null
    public var publicKey: String? = null
    public var theirPublickKey: String? = null

    fun nextKeyExchangeMessage(current: String?): MessageInfo {
        return when(current) {
            KEY_EXCHANGE_START -> MessageInfo(KEY_EXCHANGE_STEP, KEY_EXCHANGE, KEY_EXCHANGE_SYN)
            KEY_EXCHANGE_SYN -> MessageInfo(KEY_EXCHANGE_STEP, KEY_EXCHANGE, KEY_EXCHANGE_SYNACK)
            KEY_EXCHANGE_SYNACK -> MessageInfo(KEY_EXCHANGE_STEP, KEY_EXCHANGE, KEY_EXCHANGE_ACK)
            KEY_EXCHANGE_ACK -> MessageInfo(MESSAGE_STEP, null, null)
            else -> MessageInfo(KEY_EXCHANGE_STEP, KEY_EXCHANGE, KEY_EXCHANGE_SYN)
        }
    }
}