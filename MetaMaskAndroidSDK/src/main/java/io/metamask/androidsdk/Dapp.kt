package io.metamask.androidsdk
import kotlinx.serialization.Serializable

@Serializable
public data class Dapp(
    val name: String,
    val url: String
    )
