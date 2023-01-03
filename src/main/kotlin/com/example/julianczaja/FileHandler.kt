package com.example.julianczaja

import com.example.julianczaja.Constants.PHOTO_FILENAME_REGEX
import com.example.julianczaja.Constants.projectPath
import com.example.julianczaja.plugins.Photo
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField

class FileHandler {

    private val formatter: DateTimeFormatter = DateTimeFormatterBuilder()
        .appendValue(ChronoField.YEAR_OF_ERA, 4)
        .appendValue(ChronoField.MONTH_OF_YEAR, 2)
        .appendValue(ChronoField.DAY_OF_MONTH, 2)
        .appendValue(ChronoField.HOUR_OF_DAY, 2)
        .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
        .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
        .appendValue(ChronoField.MILLI_OF_SECOND, 3)
        .toFormatter()

    private fun getCurrentDateTimeString(): String = formatter.format(LocalDateTime.now())

    private fun getPhotoName(deviceId: Long) = "${deviceId}_${getCurrentDateTimeString()}.jpeg"

    private fun getPhotosDir(deviceId: Long) = "$projectPath/photos/$deviceId".replace('/', File.separatorChar)

    private fun getPhotosDir(deviceId: String) = "$projectPath/photos/$deviceId".replace('/', File.separatorChar)

    private fun createDirForDevice(deviceId: Long) {
        val devicePhotosDir = getPhotosDir(deviceId)
        Files.createDirectories(Paths.get(devicePhotosDir))
    }

    fun getDevicePhotoNamesFromDisk(fileName: String): File {
        val deviceId = fileName.split("_").first()
        val devicePhotosDir = getPhotosDir(deviceId)

        return File(devicePhotosDir, fileName)
    }

    fun getDevicePhotosNamesFromDisk(deviceId: Long, from: Long? = null, to: Long? = null): List<Photo> {
        val devicePhotosDir = getPhotosDir(deviceId)
        val photos = mutableListOf<Photo>()

        File(devicePhotosDir)
            .walk()
            .filter { it.name.matches(PHOTO_FILENAME_REGEX.toRegex()) }
            .mapTo(photos) {
                val dateTime = it.name.split("_", ".")[1]
                return@mapTo Photo(
                    deviceId = deviceId,
                    dateTime = dateTime,
                    url = "http://192.168.1.11:8123/photo/${deviceId}_${dateTime}.jpeg"
//                    url = "http://127.0.0.1:8123/photo/${deviceId}_${dateTime}.jpeg"
//                    url = "http://10.0.2.2:8123/photo/${deviceId}_${dateTime}.jpeg"
                )
            }

        return photos
    }

    suspend fun savePhotoFromChannel(deviceId: Long, channel: ByteReadChannel) {
        println("savePhotoFromChannel deviceId=$deviceId")
        createDirForDevice(deviceId)

        val fileName = getPhotoName(deviceId)
        println("savePhotoFromChannel saving in $fileName")
        val file = File(getPhotosDir(deviceId), fileName)
        channel.copyAndClose(file.writeChannel(Dispatchers.IO))
    }
}
