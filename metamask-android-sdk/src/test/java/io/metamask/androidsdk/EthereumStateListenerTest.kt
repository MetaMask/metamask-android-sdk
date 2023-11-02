package io.metamask.androidsdk

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.BeforeTest
import kotlin.test.Test


class EthereumStateListenerTest {
    private lateinit var ethereum: Ethereum

    @BeforeTest
    fun beforeEach() {
        val repo: ApplicationRepository = mockk { every { context }.returns(mockk()) }
        ethereum = Ethereum(repository = repo)
    }

    @Test
    fun testListenersAreCalledWhenNeeded() {
        val firstListener = mockk<EthereumStateListener>(relaxed = true)
        val secondListener = mockk<EthereumStateListener>(relaxed = true)

        ethereum.addStateListener(firstListener)
        ethereum.addStateListener(secondListener)

        ethereum.updateAccount("0xF")

        verify(exactly = 1) {
            firstListener.onEtheriumStateChanged(any())
        }
        verify(exactly = 1) {
            secondListener.onEtheriumStateChanged(any())
        }
    }

    @Test
    fun testListenerAreNotCalledAfterRemoval() {
        val firstListener = mockk<EthereumStateListener>(relaxed = true)
        val secondListener = mockk<EthereumStateListener>(relaxed = true)

        ethereum.addStateListener(firstListener)
        ethereum.addStateListener(secondListener)
        ethereum.removeStateListener(firstListener)

        ethereum.updateAccount("0xF")

        verify(exactly = 0) {
            firstListener.onEtheriumStateChanged(any())
        }
        verify(exactly = 1) {
            secondListener.onEtheriumStateChanged(any())
        }
    }
}