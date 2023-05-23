package com.metamask.android.sdk
import kotlinx.serialization.Serializable

@Serializable
public data class Dapp(
    val name: String,
    val url: String
    )
