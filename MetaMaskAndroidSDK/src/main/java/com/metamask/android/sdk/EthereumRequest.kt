package com.metamask.android.sdk
import kotlinx.serialization.Serializable
import java.util.*

data class EthereumRequest(
    var id: String = UUID.randomUUID().toString(),
    val method: String,
    val params: Any? = null
)
