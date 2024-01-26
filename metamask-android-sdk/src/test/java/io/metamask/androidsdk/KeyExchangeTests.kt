package io.metamask.androidsdk

import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

import io.metamask.androidsdk.KeyExchangeMessageType.*

class KeyExchangeTests {
    lateinit var keyExchange: KeyExchange

    @Before
    fun setup() {
        keyExchange = KeyExchange(MockCrypto())
    }

    @Test
    fun testStartNextKeyExchangeStepIsSYN() {
        val startKeyExchange = KeyExchangeMessage(KEY_HANDSHAKE_START.name, keyExchange.publicKey)
        val nextKeyExchangeMessage = keyExchange.nextKeyExchangeMessage(startKeyExchange)
        assertEquals(nextKeyExchangeMessage?.type, KEY_HANDSHAKE_SYN.name)
    }

    @Test
    fun testSYNNextKeyExchangeStepIsSYN_ACK() {
        val startKeyExchange = KeyExchangeMessage(KEY_HANDSHAKE_SYN.name, keyExchange.publicKey)
        val nextKeyExchangeMessage = keyExchange.nextKeyExchangeMessage(startKeyExchange)
        assertEquals(nextKeyExchangeMessage?.type, KEY_HANDSHAKE_SYNACK.name)
    }

    @Test
    fun testSYN_ACKNextKeyExchangeStepIsACK() {
        val startKeyExchange = KeyExchangeMessage(KEY_HANDSHAKE_SYNACK.name, keyExchange.publicKey)
        val nextKeyExchangeMessage = keyExchange.nextKeyExchangeMessage(startKeyExchange)
        assertEquals(nextKeyExchangeMessage?.type, KEY_HANDSHAKE_ACK.name)
    }

    @Test
    fun testACKNextKeyExchangeStepIsNull() {
        val startKeyExchange = KeyExchangeMessage(KEY_HANDSHAKE_ACK.name, keyExchange.publicKey)
        val nextKeyExchangeMessage = keyExchange.nextKeyExchangeMessage(startKeyExchange)
        assertEquals(nextKeyExchangeMessage?.type, null)
    }

    @Test
    fun testUndefinedStepNextKeyExchangeStepIsNull() {
        val startKeyExchange = KeyExchangeMessage("KEY_HANDSHAKE_RANDOM", keyExchange.publicKey)
        val nextKeyExchangeMessage = keyExchange.nextKeyExchangeMessage(startKeyExchange)
        assertEquals(nextKeyExchangeMessage?.type, null)
    }
}