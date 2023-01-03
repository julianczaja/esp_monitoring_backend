package com.example.julianczaja.plugins

import kotlinx.serialization.Serializable

@Serializable
data class Photo(
    val deviceId: Long,
    val dateTime: String,
    val url: String
)