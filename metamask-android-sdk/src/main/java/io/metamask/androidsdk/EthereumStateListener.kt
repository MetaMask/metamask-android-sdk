package io.metamask.androidsdk

fun interface EthereumStateListener {
    fun onEtheriumStateChanged(newState: EthereumState)
}