package com.metamask.android.sdk
import java.util.*

data class EthereumRequest(
    var id: String,
    val method: String,
    val params: Any? = null
)
