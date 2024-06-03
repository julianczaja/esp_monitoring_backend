package com.example.julianczaja

import UnknownDeviceException
import com.example.julianczaja.Constants.PHOTO_FILENAME_REGEX
import com.example.julianczaja.Constants.projectPath
import com.example.julianczaja.plugins.Photo
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import net.coobird.thumbnailator.Thumbnails
import java.awt.image.BufferedImage
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

    private fun getPhotosThumbnailsDir(deviceId: Long) =
        "$projectPath/photos/$deviceId/thumbnails".replace('/', File.separatorChar)

    private fun getPhotosDir(deviceId: String) = "$projectPath/photos/$deviceId".replace('/', File.separatorChar)

    private fun createDirsForDevice(deviceId: Long) {
        val devicePhotosDir = getPhotosDir(deviceId)
        val deviceThumbnailsPhotosDir = getPhotosThumbnailsDir(deviceId)

        Files.createDirectories(Paths.get(devicePhotosDir))
        Files.createDirectories(Paths.get(deviceThumbnailsPhotosDir))
    }

    fun getPhotoFile(fileName: String): File {
        val deviceId = fileName.split("_").first()
        val devicePhotosDir = getPhotosDir(deviceId)

        return File(devicePhotosDir, fileName)
    }

    fun getPhotoThumbnailFile(fileName: String): File {
        val deviceId = fileName.split("_").first()
        val devicePhotosDir = getPhotosThumbnailsDir(deviceId.toLong())

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

    fun getDevicePhotosNamesFromDisk(deviceId: Long): List<Photo> {
        val devicePhotosDir = getPhotosDir(deviceId)
        val photos = mutableListOf<Photo>()

        if (!File(devicePhotosDir).exists()) {
            throw UnknownDeviceException()
        }

        File(devicePhotosDir)
            .listFiles { file -> file.isFile }
            ?.filter { it.name.matches(PHOTO_FILENAME_REGEX.toRegex()) }
            ?.mapTo(photos) {
                val dateTime = it.name.split("_", ".")[1]
                Photo(
                    deviceId = deviceId,
                    dateTime = dateTime,
                    fileName = it.name,
                    size = getImageSizeString(it),
                    url = "http://192.168.1.57:8123/photo/${deviceId}_${dateTime}.jpeg",
                    thumbnailUrl = "http://192.168.1.57:8123/photoThumbnail/${deviceId}_${dateTime}.jpeg"
                    // url = "http://maluch2.mikr.us:$PORT/photo/${deviceId}_${dateTime}.jpeg"
                    // thumbnailUrl = "http://maluch2.mikr.us:$PORT/photoThumbnail/${deviceId}_${dateTime}.jpeg"
                )
            }

        return photos
    }

    suspend fun savePhotoFromChannel(deviceId: Long, channel: ByteReadChannel) = withContext(Dispatchers.IO) {
        createDirsForDevice(deviceId)

        val fileName = getPhotoName(deviceId)

        val originalDir = getPhotosDir(deviceId)
        val originalFile = File(originalDir, fileName)
        channel.copyAndClose(originalFile.writeChannel(Dispatchers.IO))

        val thumbnailDir = getPhotosThumbnailsDir(deviceId)
        createAndSaveThumbnail(originalFile, thumbnailDir, fileName)
    }

    private suspend fun createAndSaveThumbnail(
        fromFile: File,
        thumbnailsDir: String,
        fileName: String,
        sizePx: Int = 200
    ) = withContext(Dispatchers.IO) {
        val thumbnailImage: BufferedImage = Thumbnails.of(fromFile)
            .size(sizePx, sizePx)
            .asBufferedImage()
        val thumbnailFile = File(thumbnailsDir, fileName)

        ImageIO.write(thumbnailImage, "jpeg", thumbnailFile)
    }

    fun removePhoto(fileName: String) = File("$projectPath/photos/")
        .walk()
        .filter { it.name == fileName }
        .forEach { it.delete() }

    fun createMissingThumbnails() {
        File("$projectPath/photos/").listFiles()?.forEach { dir ->
            if (!dir.isDirectory) return@forEach

            val thumbnailPath = Files.createDirectories(Paths.get("${dir.path}/thumbnails"))

            dir.listFiles()?.forEach { file ->
                if (file.isFile) {
                    println(file.name)
                    runBlocking { createAndSaveThumbnail(file, thumbnailPath.toString(), file.name) }
                }
            }
        }
    }
}
