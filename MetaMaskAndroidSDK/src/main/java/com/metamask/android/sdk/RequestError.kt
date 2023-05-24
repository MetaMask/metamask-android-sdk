package com.metamask.android.sdk

import kotlinx.serialization.Serializable

@Serializable
data class RequestError(
    val code: Int,
    val message: String)