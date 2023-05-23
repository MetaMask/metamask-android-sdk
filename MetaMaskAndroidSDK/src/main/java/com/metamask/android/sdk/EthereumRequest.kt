package com.metamask.android.sdk
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
class EthereumRequest(
    var id: String = UUID.randomUUID().toString(),
    val method: String,
    val params: Serializable? = null
) {

}