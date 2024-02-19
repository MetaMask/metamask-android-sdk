package com.metamask.dapp
import io.metamask.androidsdk.Result

data class SnackbarData(val message: String, val action: ((callback: (Result) -> Unit) -> Unit)?)