package com.reactwallet

data class OriginatorInfo(
    val url: String,
    val title: String,
    val platform: String,
    val icon: String? = null,
    val apiVersion: String?
)