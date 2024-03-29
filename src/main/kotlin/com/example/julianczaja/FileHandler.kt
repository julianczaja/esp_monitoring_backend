package com.example.julianczaja

import com.example.julianczaja.Constants.PHOTO_FILENAME_REGEX
import com.example.julianczaja.Constants.PORT
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
import javax.imageio.ImageIO
import javax.imageio.ImageReader


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

    private fun getImageSizeString(f: File): String {
        ImageIO.createImageInputStream(f).use { input ->
            val readers: Iterator<ImageReader> = ImageIO.getImageReaders(input)
            if (readers.hasNext()) {
                val reader = readers.next()
                try {
                    reader.input = input
                    return "${reader.getWidth(0)}x${reader.getHeight(0)} px"
                } finally {
                    reader.dispose()
                }
            }
        }
        return "unknown"
    }

    fun getDevicePhotosNamesFromDisk(deviceId: Long, from: Long? = null, to: Long? = null): List<Photo> {
        val devicePhotosDir = getPhotosDir(deviceId)
        val photos = mutableListOf<Photo>()

        File(devicePhotosDir)
            .walk()
            .filter { it.name.matches(PHOTO_FILENAME_REGEX.toRegex()) }
            .mapTo(photos) {
                val dateTime = it.name.split("_", ".")[1]
                val size = getImageSizeString(it)

                return@mapTo Photo(
                    deviceId = deviceId,
                    dateTime = dateTime,
                    fileName = it.name,
                    size = size,
                    url = "http://maluch2.mikr.us:$PORT/photo/${deviceId}_${dateTime}.jpeg"
//                    url = "http://192.168.1.11:8123/photo/${deviceId}_${dateTime}.jpeg"
//                    url = "http://${Constants.BASE_URL}:${Constants.PORT}/photo/${deviceId}_${dateTime}.jpeg"
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

    fun removePhoto(fileName: String) {
        val file = File("$projectPath/photos/")
            .walk()
            .find { it.name == fileName }

        if (file != null) {
            file.delete()
        } else {
            throw Exception("File doesn't exists")
        }
    }
}
