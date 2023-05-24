package com.metamask.android.sdk
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class OriginatorInfo(
    public val title: String?,
    public val url: String?,
    public val platform: String,
    public val apiVersion: String
)