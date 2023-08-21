package io.metamask.androidsdk
import kotlinx.serialization.Serializable

@Serializable
data class Dapp(
    val name: String,
    val url: String
    )
