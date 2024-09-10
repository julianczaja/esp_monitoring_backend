package com.example.julianczaja.plugins

import com.example.julianczaja.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.freemarker.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting(fileHandler: FileHandler) {
    routing {
        getLastPhotoTemplateRoute(fileHandler)  // post      - BASE_URL/photo/{deviceId}
        uploadPhotoRoute(fileHandler)           // post      - BASE_URL/photo/{deviceId}
        getPhotosDatesRoute(fileHandler)        // get       - BASE_URL/dates/{deviceId}
        getPhotosForDateRoute(fileHandler)      // get       - BASE_URL/photos/{deviceId}/{date}
        getPhotosByNamesRoute(fileHandler)      // post      - BASE_URL/photos
        getPhotoRoute(fileHandler)              // get       - BASE_URL/photo/{filename}
        getPhotoThumbnailRoute(fileHandler)     // get       - BASE_URL/photo_thumbnail{filename}
        getLastPhotoRoute(fileHandler)          // get       - BASE_URL/last_photo/{deviceId}
        removePhotoRoute(fileHandler)           // delete    - BASE_URL/photos{filename}
        removePhotosRoute(fileHandler)          // post      - BASE_URL/photos/remove
        getDeviceInfoRoute(fileHandler)         // get       - BASE_URL/device{deviceId}
    }
}

fun Route.getLastPhotoTemplateRoute(fileHandler: FileHandler) {
    get("/last/{deviceId}") {
        try {
            val deviceIdParam = call.parameters["deviceId"]
            val deviceId = deviceIdParam?.toLongOrNull()
            if (deviceId == null) {
                call.respondText(text = "Error: Wrong deviceId: $deviceIdParam", status = HttpStatusCode.BadRequest)
                return@get
            }

            val lastPhoto = fileHandler.getDeviceLastPhotoFromDisk(deviceId)
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

fun Route.uploadPhotoRoute(fileHandler: FileHandler) {
    post("/photo/{deviceId}") {
        try {
            val deviceId = call.parameters["deviceId"]?.toLongOrNull()
            if (deviceId == null) {
                call.respondText(text = "Error: Wrong deviceId", status = HttpStatusCode.BadRequest)
                return@post
            }
            fileHandler.savePhotoFromChannel(deviceId, call.receiveChannel())
            call.respondText("Ok", status = HttpStatusCode.OK)
            fileHandler.cleanup(deviceId)
        } catch (e: Exception) {
            application.log.error("uploadPhotoRoute", e)
            call.respondText(text = "Error: ${e.message}", status = HttpStatusCode.InternalServerError)
        }
    }
}

fun Route.getPhotosDatesRoute(fileHandler: FileHandler) {
    get("/dates/{deviceId}") {
        try {
            val deviceId = call.parameters["deviceId"]?.toLongOrNull()
            if (deviceId == null) {
                call.respondText(text = "Error: Wrong deviceId", status = HttpStatusCode.BadRequest)
                return@get
            }

            val days = fileHandler.getDevicePhotosDatesFromDisk(deviceId)

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

fun Route.getPhotosForDateRoute(fileHandler: FileHandler) {
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

            val photos = fileHandler.getDevicePhotosForDateFromDisk(deviceId, date)

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

fun Route.getPhotosByNamesRoute(fileHandler: FileHandler) {
    post("/photos") {
        try {
            val params = call.receive<GetPhotosZipParams>()
            if (params.fileNames.isEmpty()) throw Exception("Empty files list in PhotosZipParams")

            val zipBytes = fileHandler.getPhotosZipFile(params.fileNames, params.isHighQuality)

            call.response.addFileNameContentDescriptionHeader("photos.zip")
            call.respondBytes(
                bytes = zipBytes,
                contentType = ContentType.Application.Zip,
                status = HttpStatusCode.OK
            )
        } catch (e: Exception) {
            application.log.error("getPhotosByNamesRoute", e)
            call.respondText(text = "Error: ${e.message}", status = HttpStatusCode.InternalServerError)
        }
    }
}

fun Route.getPhotoRoute(fileHandler: FileHandler) {
    get("/photo/{filename}") {
        try {
            val fileNameParam = call.parameters["filename"] ?: ""
            val fileName = DeviceFileName.fromStringOrNull(fileNameParam)
            if (fileName == null) {
                call.respondText(text = "Error: Wrong filename: $fileNameParam", status = HttpStatusCode.BadRequest)
                return@get
            }

            val photoFile = fileHandler.getPhotoFile(fileName)
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

fun Route.getPhotoThumbnailRoute(fileHandler: FileHandler) {
    get("/photo_thumbnail/{filename}") {
        try {
            val fileNameParam = call.parameters["filename"] ?: ""
            val fileName = DeviceFileName.fromStringOrNull(fileNameParam)
            if (fileName == null) {
                call.respondText(text = "Error: Wrong filename: $fileNameParam", status = HttpStatusCode.BadRequest)
                return@get
            }

            val photoFile = fileHandler.getPhotoThumbnailFile(fileName)
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

private fun Route.getLastPhotoRoute(fileHandler: FileHandler) {
    get("/last_photo/{deviceId}") {
        val deviceIdParam = call.parameters["deviceId"]

        try {
            val deviceId = deviceIdParam?.toLongOrNull()
            if (deviceId == null) {
                call.respondText(text = "Error: Wrong deviceId: $deviceIdParam", status = HttpStatusCode.BadRequest)
                return@get
            }

            val photo = fileHandler.getDeviceLastPhotoFromDisk(deviceId)
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

private fun Route.removePhotoRoute(fileHandler: FileHandler) {
    delete("/photos/{fileName}") {
        try {
            val fileNameParam = call.parameters["filename"] ?: ""
            val fileName = DeviceFileName.fromStringOrNull(fileNameParam)
            if (fileName == null) {
                call.respondText(text = "Error: Wrong fileName", status = HttpStatusCode.BadRequest)
                return@delete
            }

            fileHandler.removePhoto(fileName)

            call.respondText(text = "Ok", status = HttpStatusCode.OK)
        } catch (e: Exception) {
            application.log.error("removePhotoRoute", e)
            call.respondText(text = "Error: ${e.message}", status = HttpStatusCode.InternalServerError)
        }
    }
}

private fun Route.removePhotosRoute(fileHandler: FileHandler) {
    post("/photos/remove") {
        try {
            val fileNames = call.receive<List<String>>()
            if (fileNames.isEmpty()) throw Exception("Empty files list in removePhotosRoute")

            fileHandler.removePhotos(fileNames)

            call.respondText(text = "Ok", status = HttpStatusCode.OK)
        } catch (e: Exception) {
            application.log.error("removePhotosRoute", e)
            call.respondText(text = "Error: ${e.message}", status = HttpStatusCode.InternalServerError)
        }
    }
}

private fun Route.getDeviceInfoRoute(fileHandler: FileHandler) {
    get("/device/{deviceId}") {
        try {
            val deviceIdParam = call.parameters["deviceId"]
            val deviceId = deviceIdParam?.toLongOrNull()
            if (deviceId == null) {
                call.respondText(text = "Error: Wrong deviceId: $deviceIdParam", status = HttpStatusCode.BadRequest)
                return@get
            }

            val deviceInfo = fileHandler.getDeviceInfo(deviceId)

            call.respond(message = deviceInfo, status = HttpStatusCode.OK)
        } catch (e: Exception) {
            application.log.error("getDeviceInfoRoute", e)
            call.respondText(text = "Error: ${e.message}", status = HttpStatusCode.InternalServerError)
        }
    }
}
