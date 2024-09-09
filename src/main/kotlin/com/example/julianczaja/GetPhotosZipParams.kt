package com.example.julianczaja

import kotlinx.serialization.Serializable

@Serializable
data class GetPhotosZipParams(
    val fileNames: List<String>,
    var isHighQuality: Boolean
)
