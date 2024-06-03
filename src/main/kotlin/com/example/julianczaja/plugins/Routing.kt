package com.example.julianczaja.plugins

import UnknownDeviceException
import com.example.julianczaja.FileHandler
import com.example.julianczaja.addFileNameContentDescriptionHeader
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*

fun Application.configureRouting(fileHandler: FileHandler) {
    routing {
        uploadPhotoRoute(fileHandler)       // post      - BASE_URL/photo
        getPhotoRoute(fileHandler)          // get       - BASE_URL/photo
        getPhotoThumbnailRoute(fileHandler) // get       - BASE_URL/photoThumbnail
        getPhotosRoute(fileHandler)         // get       - BASE_URL/photos
        removePhotoRoute(fileHandler)       // delete    - BASE_URL/photos
        getDeviceInfoRoute(fileHandler)     // get       - BASE_URL/device
    }
}

fun Route.uploadPhotoRoute(fileHandler: FileHandler) {
    post("/photo/{deviceId}") {
        try {
            println("HEADERS = ${call.request.headers.flattenEntries()}")
            val deviceId = call.parameters["deviceId"]?.toLongOrNull()
            if (deviceId == null) {
                call.respondText(text = "Error: Wrong deviceId", status = HttpStatusCode.BadRequest)
                return@post
            }

            fileHandler.savePhotoFromChannel(deviceId, call.receiveChannel())

            call.respondText("Ok", status = HttpStatusCode.OK)
        } catch (e: Exception) {
            println("Error: ${e.message}")
            call.respondText(text = "Error: ${e.message}", status = HttpStatusCode.InternalServerError)
        }
    }
}

fun Route.getPhotoRoute(fileHandler: FileHandler) {
    get("/photo/{filename}") {
        try {
            val fileName = call.parameters["filename"]
            if (fileName == null) {
                call.respondText(text = "Error: Wrong filename", status = HttpStatusCode.BadRequest)
                return@get
            }

            val photoFile = fileHandler.getPhotoFile(fileName)
            if (!photoFile.exists()) {
                call.respondText(text = "Error: file not exists", status = HttpStatusCode.BadRequest)
            }

            call.response.addFileNameContentDescriptionHeader(fileName)
            call.respondFile(file = photoFile)
        } catch (e: Exception) {
            println("Error: ${e.message}")
            call.respondText(text = "Error: ${e.message}", status = HttpStatusCode.InternalServerError)
        }
    }
}

fun Route.getPhotoThumbnailRoute(fileHandler: FileHandler) {
    get("/photoThumbnail/{filename}") {
        try {
            val fileName = call.parameters["filename"]
            if (fileName == null) {
                call.respondText(text = "Error: Wrong filename", status = HttpStatusCode.BadRequest)
                return@get
            }

            val photoFile = fileHandler.getPhotoThumbnailFile(fileName)
            if (!photoFile.exists()) {
                call.respondText(text = "Error: file not exists", status = HttpStatusCode.BadRequest)
            }

            call.response.addFileNameContentDescriptionHeader(fileName)
            call.respondFile(file = photoFile)
        } catch (e: Exception) {
            println("Error: ${e.message}")
            call.respondText(text = "Error: ${e.message}", status = HttpStatusCode.InternalServerError)
        }
    }
}

private fun Route.getPhotosRoute(fileHandler: FileHandler) {
    get("/photos/{deviceId}") {
        try {
            println("HEADERS = ${call.request.headers.flattenEntries()}")
            val deviceId = call.parameters["deviceId"]?.toLongOrNull()
            if (deviceId == null) {
                call.respondText(text = "Error: Wrong deviceId", status = HttpStatusCode.BadRequest)
                return@get
            }

            val photos = fileHandler.getDevicePhotosNamesFromDisk(deviceId).sortedByDescending { it.dateTime }

            call.respond(message = photos, status = HttpStatusCode.OK)
        } catch (e: UnknownDeviceException) {
            call.respondText(
                text = "Error: Device ${call.parameters["deviceId"]?.toLongOrNull()} not found",
                status = HttpStatusCode.NotFound
            )
            return@get
        } catch (e: Exception) {
            println("Error: ${e.message}")
            call.respondText(text = "Error: ${e.message}", status = HttpStatusCode.InternalServerError)
        }
    }
}

private fun Route.removePhotoRoute(fileHandler: FileHandler) {
    delete("/photos/{fileName}") {
        try {
            val fileName = call.parameters["fileName"]
            if (fileName.isNullOrEmpty()) {
                call.respondText(text = "Error: Wrong fileName", status = HttpStatusCode.BadRequest)
                return@delete
            }

            fileHandler.removePhoto(fileName)

            call.respondText(text = "Ok", status = HttpStatusCode.OK)
        } catch (e: Exception) {
            println("Error: ${e.message}")
            call.respondText(text = "Error: ${e.message}", status = HttpStatusCode.InternalServerError)
        }
    }
}

fun Route.getDeviceInfoRoute(fileHandler: FileHandler) {
    get("/device/{deviceId}") {
        try {
            val deviceId = call.parameters["deviceId"]?.toLongOrNull()
            if (deviceId == null) {
                call.respondText(text = "Error: Wrong deviceId", status = HttpStatusCode.BadRequest)
                return@get
            }

            val deviceInfo = fileHandler.getDeviceInfo(deviceId)
            call.respond(message = deviceInfo, status = HttpStatusCode.OK)
        } catch (e: Exception) {
            println("Error: ${e.message}")
            call.respondText(text = "Error: ${e.message}", status = HttpStatusCode.InternalServerError)
        }
    }
}
