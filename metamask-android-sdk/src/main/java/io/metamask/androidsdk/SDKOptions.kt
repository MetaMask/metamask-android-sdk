package io.metamask.androidsdk

data class SDKOptions(
    val infuraAPIKey: String?,
    var readonlyRPCMap: Map<String, String>?
)