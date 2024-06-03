package com.example.julianczaja.plugins

import kotlinx.serialization.Serializable


@Serializable
data class DeviceInfo(
    val deviceId: Long,
    val freeSpaceMb: Float,
    val usedSpaceMb: Float,
    val lastPhotoSizeMb: Float,
    val averagePhotoSizeMb: Float,
    val photosCount: Int,
)
