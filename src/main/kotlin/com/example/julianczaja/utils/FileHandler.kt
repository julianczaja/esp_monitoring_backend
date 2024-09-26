@file:Suppress("unused", "unused")

package com.example.julianczaja.utils

import com.example.julianczaja.Configuration
import com.example.julianczaja.model.DeviceFileName
import com.example.julianczaja.model.DeviceInfo
import com.example.julianczaja.model.Photo
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object FileHandler {

    private val lastCleanupTimes: MutableMap<Long, Long> = ConcurrentHashMap()
    private val photoFileNameRegex = PHOTO_FILENAME_REGEX.toRegex()
    private val allDevicesPath = "$projectPath/photos"

    private fun getPhotoName(deviceId: Long) = "${deviceId}_$currentDateTimeString.jpeg"

    private fun getDeviceDirName(deviceId: Long) = "$allDevicesPath/$deviceId".replaceSeparators()

    fun getDeviceDir(deviceId: Long) = File(getDeviceDirName(deviceId))

    private fun getPhotosDir(deviceId: Long, date: String) =
        "$allDevicesPath/$deviceId/$date".replaceSeparators()

    private fun getPhotosThumbnailsDir(deviceId: Long, date: String) =
        "$allDevicesPath/$deviceId/$date/thumbnails".replaceSeparators()

    fun getPhotosZipFile(filesNames: List<String>, isHighQuality: Boolean): ByteArray {
        val deviceFilesNames = filesNames.mapNotNull { DeviceFileName.fromStringOrNull(it) }
        val files = deviceFilesNames.mapNotNull { fileName ->
            when {
                isHighQuality -> getPhotoFile(fileName).takeIf { it.exists() }
                else -> getPhotoThumbnailFile(fileName).takeIf { it.exists() }
            }
        }
        return createZip(files)
    }

    fun getPhotoFile(fileName: DeviceFileName): File {
        val devicePhotosDateDir = getPhotosDir(fileName.deviceId, fileName.date.toDefaultString())

        return File(devicePhotosDateDir, fileName.toString())
    }

    fun getPhotoThumbnailFile(fileName: DeviceFileName): File {
        val devicePhotosDateDir = getPhotosThumbnailsDir(fileName.deviceId, fileName.date.toDefaultString())

        return File(devicePhotosDateDir, fileName.toString())
    }

    private fun getPhotoUrl(fileName: String) = "${Configuration.fullUrl}/photo/$fileName"

    private fun getPhotoThumbnailUrl(fileName: String) = "${Configuration.fullUrl}/photo_thumbnail/$fileName"

    fun getDevicePhotosDatesFromDisk(deviceId: Long): List<String> {
        val deviceDir = getDeviceDirName(deviceId)

        if (!File(deviceDir).exists()) {
            throw UnknownDeviceException()
        }

        return File(deviceDir).listPhotoDateDirs()
            ?.map { it.name }
            ?.sortedDescending()
            ?: emptyList()
    }

    fun getDevicePhotosForDateFromDisk(deviceId: Long, date: LocalDate): List<Photo> {
        val deviceDir = getDeviceDirName(deviceId)

        if (!File(deviceDir).exists()) {
            throw UnknownDeviceException()
        }

        val devicePhotosDateDir = File(deviceDir, date.toDefaultString())

        return devicePhotosDateDir.listPhotoFiles()
            ?.sortedByDescending { it.name }
            ?.map { it.toPhoto() }
            ?: emptyList()
    }

    fun getDeviceLastPhotoFromDisk(deviceId: Long): Photo? {
        val deviceDir = File(getDeviceDirName(deviceId))

        if (!deviceDir.exists()) {
            throw UnknownDeviceException()
        }

        deviceDir.listPhotoDateDirs()
            ?.sortedByDescending { it.name }
            ?.firstOrNull()
            ?.let { newestDir ->
                newestDir.listPhotoFiles()
                    ?.sortedByDescending { it.name }
                    ?.firstOrNull()
                    ?.toPhoto()
                    ?.let { photo -> return photo }
            }

        return null
    }

    suspend fun savePhotoFromChannel(deviceId: Long, channel: ByteReadChannel) = withContext(Dispatchers.IO) {
        try {
            val fileName = getPhotoName(deviceId)
            val deviceFileName = DeviceFileName.fromStringOrNull(fileName) ?: throw Exception("Can't parse $fileName")

            val byteArray = channel.toByteArray()
            val shouldCheckMostlyBlack = SettingsManager.loadSettings(deviceId)?.detectMostlyBlackPhotos ?: false

            if (shouldCheckMostlyBlack && PhotoUtils.isPhotoMostlyBlack(byteArray)) {
                println("savePhotoFromChannel: photo is mostly black, skipping")
                return@withContext
            }

            val photosDir = getPhotosDir(deviceId, deviceFileName.date.toDefaultString())
            Files.createDirectories(Paths.get(photosDir))
            val photoFile = File(photosDir, fileName)
            photoFile.writeBytes(byteArray)

            val thumbnailsDir = getPhotosThumbnailsDir(deviceId, deviceFileName.date.toDefaultString())
            Files.createDirectories(Paths.get(thumbnailsDir))
            val thumbnailFile = File(thumbnailsDir, fileName)
            PhotoUtils.createAndSaveThumbnail(
                photoByteArray = byteArray,
                outputFile = thumbnailFile
            )
        } catch (e: Exception) {
            println("savePhotoFromChannel error: $e")
        }
    }

    fun cleanup(deviceId: Long) {
        println("$deviceId cleanup...")
        val startTime = System.currentTimeMillis()
        val lastCleanupTime = lastCleanupTimes[deviceId]

        if (lastCleanupTime != null && startTime - lastCleanupTime < Configuration.cleanupIntervalMs) {
            println("Cleanup for device $deviceId skipped. Last cleanup was less than 60 minutes ago.")
            return
        }

        val deviceDir = File(getDeviceDirName(deviceId))
        val maxSizeBytes = Configuration.maxSpaceMb * 1024 * 1024

        var totalPhotosSize = 0L
        var totalThumbnailsSize = 0L
        val photosToDelete = mutableListOf<File>()

        deviceDir.listPhotoDateDirs()
            ?.sortedByDescending { it.name }
            ?.forEach { dir ->
                dir.listPhotoFiles()?.forEach { photo ->
                    val photoSize = photo.length()
                    val thumbnail = File(dir, "thumbnails/${photo.name}")
                    val thumbnailSize = if (thumbnail.exists()) thumbnail.length() else 0L

                    totalPhotosSize += photoSize
                    totalThumbnailsSize += thumbnailSize

                    if (totalPhotosSize + totalThumbnailsSize > maxSizeBytes) {
                        photosToDelete.add(photo)
                    }
                }
            }

        val totalSize = totalPhotosSize + totalThumbnailsSize

        if (totalSize <= maxSizeBytes) {
            val currentTime = System.currentTimeMillis()
            lastCleanupTimes[deviceId] = currentTime
            println("Cleanup done in ${currentTime - startTime}ms")
            return
        }

        println("Starting deletion...")

        var currentSize = totalSize

        photosToDelete.sortByDescending { it.name }
        photosToDelete.forEach { photo ->
            if (currentSize <= maxSizeBytes) return@forEach

            val thumbnailFile = File(photo.parentFile, "thumbnails/${photo.name}")
            if (thumbnailFile.exists()) {
                currentSize -= thumbnailFile.length()
                thumbnailFile.delete()
            }

            currentSize -= photo.length()
            photo.delete()
        }

        deviceDir.walkBottomUp().forEach { folder ->
            if (folder.isDirectory && folder.listFiles()?.isEmpty() == true) {
                folder.delete()
            }
        }

        val currentTime = System.currentTimeMillis()
        lastCleanupTimes[deviceId] = currentTime
        println("Cleanup done in ${currentTime - startTime}ms")
    }

    fun removePhoto(fileName: DeviceFileName) {
        val deviceDir = File(getDeviceDirName(fileName.deviceId))

        deviceDir.listPhotoDateDirs()
            ?.first { it.name == fileName.date.toDefaultString() }
            ?.also { photoDateDir ->
                photoDateDir
                    .walk()
                    .filter { it.name == fileName.toString() }
                    .forEach { it.delete() }
            }
    }

    fun removePhotos(fileNames: List<String>) {
        val devicesFileNames = fileNames.mapNotNull { DeviceFileName.fromStringOrNull(it) }

        devicesFileNames
            .groupBy { it.deviceId }
            .map { it.key to it.value.toString() }
            .forEach { (deviceId, deviceFileNames) ->
                File(getDeviceDirName(deviceId))
                    .walk()
                    .filter { it.name in deviceFileNames }
                    .forEach { it.delete() }
            }
    }

    fun getDeviceInfo(deviceId: Long): DeviceInfo {
        val deviceDir = File(getDeviceDirName(deviceId))

        val averageSumCount = 10
        var usedSpace = 0L
        var lastPhotoSize = 0L
        var averagePhotoSize = 0L
        var photosCount = 0
        var newestPhotoTimestamp: Long? = null
        var oldestPhotoTimestamp: Long? = null

        deviceDir.listPhotoDateDirs()
            ?.sortedByDescending { it.name }
            ?.also { dirs ->
                val newestDir = dirs.firstOrNull()
                val oldestDir = dirs.lastOrNull()

                newestDir?.listPhotoFiles()
                    ?.sortedByDescending { it.name }
                    ?.also { sorted ->
                        sorted.firstOrNull()?.let { newestPhotoFile ->
                            newestPhotoTimestamp = getTimestampFromPhotoFileName(newestPhotoFile.name)
                        }
                    }
                    ?.also { sorted ->
                        val lastPhotos = sorted.take(averageSumCount)
                        lastPhotoSize = lastPhotos.firstOrNull()?.length() ?: 0L
                        averagePhotoSize = lastPhotos.sumOf { it.length() } / lastPhotos.size
                    }

                oldestDir?.listPhotoFiles()
                    ?.sortedBy { it.name }
                    ?.also { sorted ->
                        sorted.firstOrNull()?.let { oldestPhotoFile ->
                            oldestPhotoTimestamp = getTimestampFromPhotoFileName(oldestPhotoFile.name)
                        }
                    }
            }
            ?.forEach { photosDateDir ->
                photosDateDir.listPhotoFiles()
                    ?.also { photoFiles ->
                        usedSpace += photoFiles.sumOf { it.length() }
                        photosCount += photoFiles.size
                    }
            }

        val maxSpaceMb = Configuration.maxSpaceMb.toFloat()
        val freeSpaceMb = maxSpaceMb - usedSpace.bytesToMegaBytes()

        return DeviceInfo(
            deviceId = deviceId,
            freeSpaceMb = freeSpaceMb,
            usedSpaceMb = usedSpace.bytesToMegaBytes(),
            spaceLimitMb = maxSpaceMb,
            lastPhotoSizeMb = lastPhotoSize.bytesToMegaBytes(),
            averagePhotoSizeMb = averagePhotoSize.bytesToMegaBytes(),
            photosCount = photosCount,
            newestPhotoTimestamp = newestPhotoTimestamp,
            oldestPhotoTimestamp = oldestPhotoTimestamp
        )
    }

    fun createMissingThumbnails() {
        File(allDevicesPath).listFiles()?.forEach { dir ->
            if (!dir.isDirectory) return@forEach

            val thumbnailPath = Files.createDirectories(Paths.get("${dir.path}/thumbnails"))

            dir.listFiles()?.forEach { photoFile ->
                if (photoFile.isFile) {
                    PhotoUtils.createAndSaveThumbnail(
                        fullPhotoFile = photoFile,
                        outputFile = File(thumbnailPath.toString(), photoFile.name)
                    )
                }
            }
        }
    }

    fun movePhotosToCorrectDirs(deviceId: Long? = null) {
        println("movePhotosToCorrectDirs")
        File(allDevicesPath)
            .listFiles { file -> file.isDirectory }
            ?.forEach { photosDir ->
                if (deviceId != null && photosDir.name != deviceId.toString()) return

                // photos
                moveFilesToDateDirs(photosDir, photosDir)

                // thumbnails
                val thumbnailsDir = File(photosDir, "thumbnails")
                if (thumbnailsDir.exists()) {
                    moveFilesToDateDirs(thumbnailsDir, photosDir)
                }
                thumbnailsDir.delete()
            }
    }

    private fun moveFilesToDateDirs(sourceDir: File, targetParentDir: File) {
        println("moveFilesToDateDirs sourceDir->targetParentDir")
        sourceDir
            .listFiles { file -> file.isFile }
            ?.filter { it.name.matches(photoFileNameRegex) }
            ?.forEach { photoFile ->
                val deviceFileName = DeviceFileName.fromStringOrNull(photoFile.name)
                    ?: throw Exception("Can't parse ${photoFile.name}")

                val date = deviceFileName.date.toDefaultString()
                val dateDir = File(targetParentDir, date)
                if (!dateDir.exists()) {
                    dateDir.mkdirs()
                }

                val targetFilePath = when (sourceDir.name) {
                    "thumbnails" -> File(dateDir, "thumbnails/${photoFile.name}".replaceSeparators())
                    else -> File(dateDir, photoFile.name)
                }

                println("Moving $photoFile to $targetFilePath")
                photoFile.copyTo(targetFilePath)
                photoFile.delete()
            }
    }

    private fun getTimestampFromPhotoFileName(fileName: String): Long? = try {
        DeviceFileName.fromStringOrNull(fileName)?.dateTime?.toInstant(ZoneOffset.UTC)?.toEpochMilli()
    } catch (e: Exception) {
        println("getTimestampFromPhotoFileName error: can't convert '$fileName'")
        null
    }

    private fun createZip(files: List<File>): ByteArray {
        val outputStream = ByteArrayOutputStream()
        ZipOutputStream(outputStream).use { zipOut ->
            files.forEach { file ->
                val zipEntry = ZipEntry(file.name)
                zipOut.putNextEntry(zipEntry)
                file.inputStream().use { input ->
                    input.copyTo(zipOut)
                }
                zipOut.closeEntry()
            }
        }
        return outputStream.toByteArray()
    }

    private fun File.listPhotoDateDirs() = this.listFiles { file -> file.isDirectory && file.name.count() == 8 }

    private fun File.listPhotoFiles() = this.listFiles { file -> file.isFile && file.name.matches(photoFileNameRegex) }

    private fun String.replaceSeparators() = this.replace('/', File.separatorChar)

    private fun File.toPhoto(): Photo {
        val deviceFileName = DeviceFileName.fromStringOrNull(this.name) ?: throw Exception("Can't parse ${this.name}")
        val deviceId = deviceFileName.deviceId
        val dateTime = deviceFileName.dateTime.toDefaultString()

        return Photo(
            deviceId = deviceId,
            dateTime = dateTime,
            fileName = this.name,
            size = PhotoUtils.getImageSizeString(this),
            url = getPhotoUrl(deviceFileName.toString()),
            thumbnailUrl = getPhotoThumbnailUrl(deviceFileName.toString()),
        )
    }
}
