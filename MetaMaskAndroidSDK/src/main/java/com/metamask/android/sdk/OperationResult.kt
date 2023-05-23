package com.metamask.android.sdk

sealed class OperationResult<out T>
data class Success<out T>(val value: T) : OperationResult<T>()
data class Error(val message: String) : OperationResult<Nothing>()