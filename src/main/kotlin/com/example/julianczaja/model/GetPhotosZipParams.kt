package com.example.julianczaja.model

import kotlinx.serialization.Serializable

@Serializable
data class GetPhotosZipParams(
    val fileNames: List<String>,
    var isHighQuality: Boolean
)
