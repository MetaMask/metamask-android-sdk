package io.metamask.androidsdk
import java.util.*

data class EthereumRequest(
    var id: String = UUID.randomUUID().toString(),
    val method: String,
    val params: Any? = null
)
