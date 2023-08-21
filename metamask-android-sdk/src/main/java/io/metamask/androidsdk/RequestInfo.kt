package io.metamask.androidsdk

import kotlinx.serialization.Serializable

@Serializable
data class RequestInfo(
    val type: String,
    val originatorInfo: OriginatorInfo
)