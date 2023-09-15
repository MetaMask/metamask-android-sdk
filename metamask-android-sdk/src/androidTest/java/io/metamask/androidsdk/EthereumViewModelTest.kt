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
class EthereumViewModelTest {

    private lateinit var context: Context
    private lateinit var ethereumViewModel: EthereumViewModel

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        ethereumViewModel = EthereumViewModel(ApplicationRepository(context))
    }

    @Test
    fun testSelectedAddressInitiallyEmpty() {
        assertEquals(ethereumViewModel.selectedAddress, "")
    }

    @Test
    fun testChainIdInitiallyEmpty() {
        assertEquals(ethereumViewModel.chainId, "")
    }

    @Test
    fun testUpdateSelectedAddress() {
        val account = "0x123456789"
        ethereumViewModel.updateAccount(account)
        assertEquals(ethereumViewModel.selectedAddress, account)
    }

    @Test
    fun testUpdateChainId() {
        val chainId = "0x1"
        ethereumViewModel.updateChainId(chainId)
        assertEquals(ethereumViewModel.chainId, chainId)
    }

    @Test
    fun testEthereumStateUpdatesOnSelectedAddressChange() {
        val account = "0x987654321"
        ethereumViewModel.updateAccount(account)

        val ethereumState = ethereumViewModel.ethereumState.value
        assertEquals(account, ethereumState?.selectedAddress)
    }

    @Test
    fun testEthereumStateUpdatesOnChainIdChange() {
        val chainId = "0x89"
        ethereumViewModel.updateChainId(chainId)

        val ethereumState = ethereumViewModel.ethereumState.value
        assertEquals(chainId, ethereumState?.chainId)
    }

    @Test
    fun testEthereumStateSessionIdPersistsOnSelectedAddressChange() {
        val account = "0x148765329"

        val ethereumState = ethereumViewModel.ethereumState.value
        val sessionIdBeforeUpdate = ethereumState?.sessionId

        ethereumViewModel.updateAccount(account)
        val sessionIdAfterUpdate = ethereumState?.sessionId
        assertEquals(sessionIdBeforeUpdate, sessionIdAfterUpdate)
    }

    @Test
    fun testEthereumStateSessionIdPersistsOnChainIdChange() {
        val chainId = "0x1"

        val ethereumState = ethereumViewModel.ethereumState.value
        val sessionIdBeforeUpdate = ethereumState?.sessionId

        ethereumViewModel.updateChainId(chainId)

        val sessionIdAfterUpdate = ethereumState?.sessionId
        assertEquals(sessionIdBeforeUpdate, sessionIdAfterUpdate)
    }

    @Test
    fun testEthereumStateSessionIdSameAsGetSessionId() {
        val ethereumState = ethereumViewModel.ethereumState.value
        val ethereumStateSessionId = ethereumState?.sessionId
        val getSessionIdSessionId = ethereumViewModel.getSessionId()
        assertEquals(ethereumStateSessionId, getSessionIdSessionId)
    }

    @Test
    fun testEthereumStateValuesSameAsInstanceValues() {
        val chainId = "0x1"
        val account = "0x123456789"
        val sessionId = ethereumViewModel.getSessionId()
        ethereumViewModel.updateChainId(chainId)
        ethereumViewModel.updateAccount(account)

        val ethereumState = ethereumViewModel.ethereumState.value

        val ethereumStateChainId = ethereumState?.chainId
        val ethereumStateSelectedAddress = ethereumState?.selectedAddress
        val ethereumStateSessionId = ethereumState?.sessionId

        assertEquals(sessionId, ethereumStateSessionId)
        assertEquals(chainId, ethereumStateChainId)
        assertEquals(account, ethereumStateSelectedAddress)
    }

    @Test
    fun testEthereumStateResetsOnClearSession() {
        val chainId = "0x1"
        val account = "0x123456789"
        val sessionId = ethereumViewModel.getSessionId()
        val ethereumState = ethereumViewModel.ethereumState.value

        ethereumViewModel.updateChainId(chainId)
        ethereumViewModel.updateAccount(account)
        ethereumViewModel.clearSession()

        assertEquals(ethereumViewModel.chainId, "")
        assertEquals(ethereumViewModel.selectedAddress, "")
        assertEquals(ethereumState?.selectedAddress, "")
        assertEquals(ethereumState?.chainId, "")

        assertNotEquals(ethereumViewModel.getSessionId(), sessionId)
    }

    @Test
    fun testSessionIdChangesOnClearSession() {
        val beforeResetSessionId = ethereumViewModel.getSessionId()
        ethereumViewModel.clearSession()
        val afterResetSessionId = ethereumViewModel.getSessionId()
        assertNotEquals(beforeResetSessionId, afterResetSessionId)
    }

    @Test
    fun testSendRequest() {

    }
}