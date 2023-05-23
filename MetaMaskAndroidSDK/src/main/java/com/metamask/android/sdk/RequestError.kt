package com.metamask.android.sdk

import kotlinx.serialization.Serializable

@Serializable
class RequestError(data: Map<String, Any>) {
    val code: Int = data["code"] as? Int ?: -1
    val messsage: String = data["message"] as? String ?: ErrorType.message(code)
}