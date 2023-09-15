package io.metamask.androidsdk

import org.junit.Test
import org.junit.Before
import org.junit.Assert.*
import org.junit.runner.RunWith

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.Rule

@RunWith(AndroidJUnit4::class)
class CommunicationClientTest {
    private lateinit var context: Context
    private lateinit var communicationClient: CommunicationClient
    private lateinit var ethereumEventCallbackMock: EthereumEventCallbackMock

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        ethereumEventCallbackMock = EthereumEventCallbackMock()
        communicationClient = CommunicationClient(context, ethereumEventCallbackMock)
    }
}

class EthereumEventCallbackMock: EthereumEventCallback {
    lateinit var chainId: String
    lateinit var selectedAddress: String

    override fun updateAccount(account: String) {
        selectedAddress = account
    }

    override fun updateChainId(newChainId: String) {
        chainId = newChainId
    }
}