package com.example.julianczaja.model

import kotlinx.serialization.Serializable


@Serializable
data class DeviceInfo(
    val deviceId: Long,
    val freeSpaceMb: Float,
    val usedSpaceMb: Float,
    val spaceLimitMb: Float,
    val lastPhotoSizeMb: Float,
    val averagePhotoSizeMb: Float,
    val photosCount: Int,
    val newestPhotoTimestamp: Long?,
    val oldestPhotoTimestamp: Long?,
)
