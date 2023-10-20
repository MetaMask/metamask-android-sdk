package io.metamask.androidsdk.coroutines

import io.metamask.androidsdk.ApplicationRepository
import io.metamask.androidsdk.Ethereum
import io.metamask.androidsdk.EthereumState
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class EthereumExtTest {
    private lateinit var ethereum: Ethereum

    private val dispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun beforeEach() {
        val repo: ApplicationRepository = mockk { every { context }.returns(mockk()) }
        ethereum = Ethereum(repository = repo)
    }

    @Test
    fun testFlowIsCalledOnStateChange() = runTest(dispatcher) {
        val result = mutableListOf<EthereumState>()

        val collectJob = ethereum.etheriumStateFlow.onEach(result::add).launchIn(this)
        advanceUntilIdle()

        ethereum.updateAccount("0xF")
        ethereum.updateAccount("0x2")
        collectJob.cancel()
        advanceUntilIdle()

        assertTrue(result.size == 2)
        assertEquals("0xF", result.first().selectedAddress)
        assertEquals("0x2", result.last().selectedAddress)
    }
}