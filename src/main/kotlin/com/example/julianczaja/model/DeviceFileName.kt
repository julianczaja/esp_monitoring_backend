package com.example.julianczaja.model

import com.example.julianczaja.utils.PHOTO_FILENAME_REGEX
import com.example.julianczaja.utils.toDefaultString
import com.example.julianczaja.utils.toLocalDateTime
import java.time.LocalDate
import java.time.LocalDateTime


data class DeviceFileName(
    val deviceId: Long,
    val dateTime: LocalDateTime,
) {
    companion object {

        // ex. '1_20230421144939007.jpeg'
        fun fromStringOrNull(string: String) = try {
            when {
                string.matches(PHOTO_FILENAME_REGEX.toRegex()) -> DeviceFileName(
                    deviceId = string.split("_")[0].toLong(),
                    dateTime = string.split("_")[1].split(".")[0].toLocalDateTime(),
                )

                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    val date: LocalDate = dateTime.toLocalDate()

    override fun toString() = "${deviceId}_${dateTime.toDefaultString()}.jpeg"
}
