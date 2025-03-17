package com.example.julianczaja.utils

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.JpegWriter
import com.sksamuel.scrimage.pixels.Pixel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File
import javax.imageio.ImageIO

object PhotoUtils {

    suspend fun isPhotoMostlyBlack(
        byteArray: ByteArray,
        thresholdPercentage: Double = BLACK_PHOTO_THRESHOLD
    ): Boolean = withContext(Dispatchers.IO) {
        val image = ImmutableImage.loader().fromStream(ByteArrayInputStream(byteArray))
        val totalPixels = image.width * image.height
        var blackPixels = 0

        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                val pixel = image.pixel(x, y)
                if (isPixelBlack(pixel)) {
                    blackPixels++
                }
            }
        }

        val blackPercentage = (blackPixels.toDouble() / totalPixels) * 100

        return@withContext blackPercentage >= thresholdPercentage
    }

    fun createAndSaveThumbnail(
        fullPhotoFile: File,
        thumbnailWidth: Int = THUMBNAIL_SIZE_PX,
        outputFile: File
    ) {
        val image = ImmutableImage.loader().fromFile(fullPhotoFile)
        val thumbnail = image.scaleToWidth(thumbnailWidth)

        thumbnail.output(JpegWriter.Default, outputFile)
    }

    fun createAndSaveThumbnail(
        photoByteArray: ByteArray,
        thumbnailWidth: Int = THUMBNAIL_SIZE_PX,
        outputFile: File
    ) {
        val image = ImmutableImage.loader().fromBytes(photoByteArray)
        val thumbnail = image.scaleToWidth(thumbnailWidth)

        thumbnail.output(JpegWriter.Default, outputFile)
    }

    fun getImageSizeString(file: File) = try {
        ImageIO.createImageInputStream(file).use { input ->
            val readers = ImageIO.getImageReaders(input)
            if (readers.hasNext()) {
                val reader = readers.next()
                reader.input = input
                val width = reader.getWidth(0)
                val height = reader.getHeight(0)
                reader.dispose()
                return "${width}x${height} px"
            } else {
                "unknown"
            }
        }
    } catch (_: Exception) {
        "unknown"
    }

    private fun isPixelBlack(pixel: Pixel) = pixel.red() <= BLACK_PIXEL_THRESHOLD
            && pixel.green() <= BLACK_PIXEL_THRESHOLD
            && pixel.blue() <= BLACK_PIXEL_THRESHOLD
}
