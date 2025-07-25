package com.example.julianczaja.model

import kotlinx.serialization.Serializable

@Serializable
data class Photo(
    val deviceId: Long,
    val dateTime: String,
    val fileName: String,
    val size: String,
    val url: String,
    val thumbnailUrl: String
)
