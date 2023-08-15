package com.metamask.dapp

data class SnackbarData(val message: String, val action: (() -> Unit)?)