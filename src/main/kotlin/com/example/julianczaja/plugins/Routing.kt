package com.example.julianczaja.plugins

import com.example.julianczaja.model.DeviceFileName
import com.example.julianczaja.model.DeviceServerSettings
import com.example.julianczaja.model.GetPhotosZipParams
import com.example.julianczaja.utils.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.freemarker.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        getLastPhotoTemplateRoute()         // post      - BASE_URL/photo/{deviceId}
        uploadPhotoRoute()                  // post      - BASE_URL/photo/{deviceId}
        getPhotosDatesRoute()               // get       - BASE_URL/dates/{deviceId}
        getPhotosForDateRoute()             // get       - BASE_URL/photos/{deviceId}/{date}
        getPhotosByNamesRoute()             // post      - BASE_URL/photos
        getPhotoRoute()                     // get       - BASE_URL/photo/{filename}
        getPhotoThumbnailRoute()            // get       - BASE_URL/photo_thumbnail{filename}
        getLastPhotoRoute()                 // get       - BASE_URL/last_photo/{deviceId}
        removePhotoRoute()                  // delete    - BASE_URL/photos{filename}
        removePhotosRoute()                 // post      - BASE_URL/photos/remove
        getDeviceInfoRoute()                // get       - BASE_URL/device/{deviceId}
        getDeviceServerSettingsRoute()      // get       - BASE_URL/device/{deviceId}/settings
        updateDeviceServerSettingsRoute()   // post      - BASE_URL/device/{deviceId}/settings
    }
}

fun Route.getLastPhotoTemplateRoute() {
    get("/last/{deviceId}") {
        try {
            val deviceIdParam = call.parameters["deviceId"]
            val deviceId = deviceIdParam?.toLongOrNull()
            if (deviceId == null) {
                call.respondText(text = "Error: Wrong deviceId: $deviceIdParam", status = HttpStatusCode.BadRequest)
                return@get
            }

            val lastPhoto = FileHandler.getDeviceLastPhotoFromDisk(deviceId)
            if (lastPhoto == null) {
                call.respondText(
                    text = "Error: Last photo of device $deviceIdParam not found",
                    status = HttpStatusCode.NotFound
                )
            } else {
                val content = FreeMarkerContent(
                    "index.ftl",
                    mapOf(
                        "url" to lastPhoto.url,
                        "deviceId" to lastPhoto.deviceId,
                        "size" to lastPhoto.size,
                        "dateTime" to lastPhoto.dateTime.toLocalDateTime().toPrettyString(),
                    )
                )
                call.respond(content)
            }
        } catch (e: UnknownDeviceException) {
            application.log.error("getLastPhotoTemplateRoute", e)
            call.respondText(
                text = "Error: unknown device (id = ${call.parameters["deviceId"]})",
                status = HttpStatusCode.InternalServerError
            )
        } catch (e: Exception) {
            application.log.error("getLastPhotoTemplateRoute", e)
            call.respondText(text = "Error: ${e.message}", status = HttpStatusCode.InternalServerError)
        }
    }
}

fun Route.uploadPhotoRoute() {
    post("/photo/{deviceId}") {
        try {
            val deviceId = call.parameters["deviceId"]?.toLongOrNull()
            if (deviceId == null) {
                call.respondText(text = "Error: Wrong deviceId", status = HttpStatusCode.BadRequest)
                return@post
            }
            FileHandler.savePhotoFromChannel(deviceId, call.receiveChannel())
            call.respondText("Ok", status = HttpStatusCode.OK)
            FileHandler.cleanup(deviceId)
        } catch (e: Exception) {
            application.log.error("uploadPhotoRoute", e)
            call.respondText(text = "Error: ${e.message}", status = HttpStatusCode.InternalServerError)
        }
    }
}

fun Route.getPhotosDatesRoute() {
    get("/dates/{deviceId}") {
        try {
            val deviceId = call.parameters["deviceId"]?.toLongOrNull()
            if (deviceId == null) {
                call.respondText(text = "Error: Wrong deviceId", status = HttpStatusCode.BadRequest)
                return@get
            }

            val days = FileHandler.getDevicePhotosDatesFromDisk(deviceId)

            call.respond(message = days, status = HttpStatusCode.OK)
        } catch (e: UnknownDeviceException) {
            call.respondText(
                text = "Error: Device ${call.parameters["deviceId"]?.toLongOrNull()} not found",
                status = HttpStatusCode.NotFound
            )
            return@get
        } catch (e: Exception) {
            application.log.error("getPhotosRoute", e)
            call.respondText(text = "Error: ${e.message}", status = HttpStatusCode.InternalServerError)
        }
    }
}

fun Route.getPhotosForDateRoute() {
    get("/photos/{deviceId}/{date}") {
        try {
            val deviceIdParam = call.parameters["deviceId"]
            val deviceId = deviceIdParam?.toLongOrNull()
            if (deviceId == null) {
                call.respondText(text = "Error: Wrong deviceId: $deviceIdParam", status = HttpStatusCode.BadRequest)
                return@get
            }

            val dateParam = call.parameters["date"]
            val date = dateParam?.toLocalDateOrNull()
            if (date == null) {
                call.respondText(text = "Error: Wrong date: $dateParam", status = HttpStatusCode.BadRequest)
                return@get
            }

            val photos = FileHandler.getDevicePhotosForDateFromDisk(deviceId, date)

            call.respond(message = photos, status = HttpStatusCode.OK)
        } catch (e: UnknownDeviceException) {
            call.respondText(
                text = "Error: Device ${call.parameters["deviceId"]?.toLongOrNull()} not found",
                status = HttpStatusCode.NotFound
            )
            return@get
        } catch (e: Exception) {
            application.log.error("getPhotosForDateRoute", e)
            call.respondText(text = "Error: ${e.message}", status = HttpStatusCode.InternalServerError)
        }
    }
}

fun Route.getPhotosByNamesRoute() {
    post("/photos") {
        try {
            val params = call.receive<GetPhotosZipParams>()
            if (params.fileNames.isEmpty()) throw Exception("Empty files list in PhotosZipParams")

            val photosFiles = FileHandler.getPhotosFilesForZip(params.fileNames, params.isHighQuality)

            call.response.addFileNameContentDescriptionHeader("photos.zip")
            call.respondOutputStream(
                contentType = ContentType.Application.Zip,
                producer = {
                    FileHandler.sendPhotosZipToStream(photosFiles, this)
                }
            )
        } catch (e: Exception) {
            application.log.error("getPhotosByNamesRoute", e)
            call.respondText(text = "Error: ${e.message}", status = HttpStatusCode.InternalServerError)
        }
    }
}

fun Route.getPhotoRoute() {
    get("/photo/{filename}") {
        try {
            val fileNameParam = call.parameters["filename"] ?: ""
            val fileName = DeviceFileName.fromStringOrNull(fileNameParam)
            if (fileName == null) {
                call.respondText(text = "Error: Wrong filename: $fileNameParam", status = HttpStatusCode.BadRequest)
                return@get
            }

            val photoFile = FileHandler.getPhotoFile(fileName)
            if (!photoFile.exists()) {
                call.respondText(text = "Error: file not exists: $fileNameParam", status = HttpStatusCode.BadRequest)
            }

            call.response.addFileNameContentDescriptionHeader(fileName.toString())
            call.respondFile(file = photoFile)
        } catch (e: Exception) {
            application.log.error("getPhotoRoute", e)
            call.respondText(text = "Error: ${e.message}", status = HttpStatusCode.InternalServerError)
        }
    }
}

fun Route.getPhotoThumbnailRoute() {
    get("/photo_thumbnail/{filename}") {
        try {
            val fileNameParam = call.parameters["filename"] ?: ""
            val fileName = DeviceFileName.fromStringOrNull(fileNameParam)
            if (fileName == null) {
                call.respondText(text = "Error: Wrong filename: $fileNameParam", status = HttpStatusCode.BadRequest)
                return@get
            }

            val photoFile = FileHandler.getPhotoThumbnailFile(fileName)
            if (!photoFile.exists()) {
                call.respondText(text = "Error: file not exists: $photoFile", status = HttpStatusCode.BadRequest)
            }

            call.response.addFileNameContentDescriptionHeader(fileName.toString())
            call.respondFile(file = photoFile)
        } catch (e: Exception) {
            application.log.error("getPhotoThumbnailRoute", e)
            call.respondText(text = "Error: ${e.message}", status = HttpStatusCode.InternalServerError)
        }
    }
}

private fun Route.getLastPhotoRoute() {
    get("/last_photo/{deviceId}") {
        val deviceIdParam = call.parameters["deviceId"]

        try {
            val deviceId = deviceIdParam?.toLongOrNull()
            if (deviceId == null) {
                call.respondText(text = "Error: Wrong deviceId: $deviceIdParam", status = HttpStatusCode.BadRequest)
                return@get
            }

            val photo = FileHandler.getDeviceLastPhotoFromDisk(deviceId)
            if (photo == null) {
                call.respondText(
                    text = "Error: Last photo of device $deviceIdParam not found",
                    status = HttpStatusCode.NotFound
                )
            } else {
                call.respond(message = photo, status = HttpStatusCode.OK)
            }

        } catch (e: UnknownDeviceException) {
            call.respondText(
                text = "Error: Device $deviceIdParam not found",
                status = HttpStatusCode.NotFound
            )
            return@get
        } catch (e: Exception) {
            application.log.error("getPhotosRoute", e)
            call.respondText(text = "Error: ${e.message}", status = HttpStatusCode.InternalServerError)
        }
    }
}

private fun Route.removePhotoRoute() {
    delete("/photos/{fileName}") {
        try {
            val fileNameParam = call.parameters["filename"] ?: ""
            val fileName = DeviceFileName.fromStringOrNull(fileNameParam)
            if (fileName == null) {
                call.respondText(text = "Error: Wrong fileName", status = HttpStatusCode.BadRequest)
                return@delete
            }

            FileHandler.removePhoto(fileName)

            call.respondText(text = "Ok", status = HttpStatusCode.OK)
        } catch (e: Exception) {
            application.log.error("removePhotoRoute", e)
            call.respondText(text = "Error: ${e.message}", status = HttpStatusCode.InternalServerError)
        }
    }
}

private fun Route.removePhotosRoute() {
    post("/photos/remove") {
        try {
            val fileNames = call.receive<List<String>>()
            if (fileNames.isEmpty()) throw Exception("Empty files list in removePhotosRoute")

            FileHandler.removePhotos(fileNames)

            call.respondText(text = "Ok", status = HttpStatusCode.OK)
        } catch (e: Exception) {
            application.log.error("removePhotosRoute", e)
            call.respondText(text = "Error: ${e.message}", status = HttpStatusCode.InternalServerError)
        }
    }
}

private fun Route.getDeviceInfoRoute() {
    get("/device/{deviceId}") {
        try {
            val deviceIdParam = call.parameters["deviceId"]
            val deviceId = deviceIdParam?.toLongOrNull()
            if (deviceId == null) {
                call.respondText(text = "Error: Wrong deviceId: $deviceIdParam", status = HttpStatusCode.BadRequest)
                return@get
            }

            val deviceInfo = FileHandler.getDeviceInfo(deviceId)

            call.respond(message = deviceInfo, status = HttpStatusCode.OK)
        } catch (e: Exception) {
            application.log.error("getDeviceInfoRoute", e)
            call.respondText(text = "Error: ${e.message}", status = HttpStatusCode.InternalServerError)
        }
    }
}

private fun Route.getDeviceServerSettingsRoute() {
    get("/device/{deviceId}/settings") {
        try {
            val deviceIdParam = call.parameters["deviceId"]
            val deviceId = deviceIdParam?.toLongOrNull()
            if (deviceId == null) {
                call.respondText(text = "Error: Wrong deviceId: $deviceIdParam", status = HttpStatusCode.BadRequest)
                return@get
            }

            val settings = SettingsManager.loadSettings(deviceId) ?: throw Exception("empty device settings")

            call.respond(message = settings, status = HttpStatusCode.OK)
        } catch (e: Exception) {
            application.log.error("getDeviceServerSettingsRoute", e)
            call.respondText(text = "Error: ${e.message}", status = HttpStatusCode.InternalServerError)
        }
    }
}

private fun Route.updateDeviceServerSettingsRoute() {
    post("/device/{deviceId}/settings") {
        try {
            val deviceIdParam = call.parameters["deviceId"]
            val deviceId = deviceIdParam?.toLongOrNull()
            if (deviceId == null) {
                call.respondText(text = "Error: Wrong deviceId: $deviceIdParam", status = HttpStatusCode.BadRequest)
                return@post
            }

            val settings = call.receive<DeviceServerSettings>()
            SettingsManager.saveSettings(deviceId, settings)

            val newSettings = SettingsManager.loadSettings(deviceId) ?: throw Exception("Cannot load settings from file")
            call.respond(message = newSettings,  status = HttpStatusCode.OK)
        } catch (e: Exception) {
            application.log.error("updateDeviceServerSettingsRoute", e)
            call.respondText(text = "Error: ${e.message}", status = HttpStatusCode.InternalServerError)
        }
    }
}
