package com.example.julianczaja.model

import kotlinx.serialization.Serializable

@Serializable
data class DeviceServerSettings(
    val detectMostlyBlackPhotos: Boolean = false,
)
