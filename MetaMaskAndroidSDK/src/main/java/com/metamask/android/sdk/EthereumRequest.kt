package com.metamask.android.sdk
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
class EthereumRequest<T>(
    var id: String = UUID.randomUUID().toString(),
    val method: String,
    val params: T? = null
)
