package io.metamask.androidsdk

import kotlinx.serialization.Serializable

@Serializable
data class RequestError(
    val code: Int,
    val message: String)