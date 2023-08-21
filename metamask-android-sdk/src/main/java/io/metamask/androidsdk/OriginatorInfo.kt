package io.metamask.androidsdk
import kotlinx.serialization.Serializable

@Serializable
data class OriginatorInfo(
    val title: String?,
    val url: String?,
    val platform: String,
    val apiVersion: String
)