package com.example.julianczaja.plugins

import com.example.julianczaja.FileHandler
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*

fun Application.configureRouting(fileHandler: FileHandler) {
    routing {
        uploadPhotoRoute(fileHandler)  // post - BASE_URL/photo
        getPhotoRoute(fileHandler)     // get  - BASE_URL/photo
        getPhotosRoute(fileHandler)    // get  - BASE_URL/photos
    }
}

fun Route.uploadPhotoRoute(fileHandler: FileHandler) {
    post("/photo/{deviceId}") {
        try {
            println("HEADERS = ${call.request.headers.flattenEntries()}")
            val deviceId = call.parameters["deviceId"]?.toLongOrNull()
            if (deviceId == null) {
                call.respondText("Error: Wrong deviceId", status = HttpStatusCode.BadRequest)
                return@post
            }

            fileHandler.savePhotoFromChannel(deviceId, call.receiveChannel())

            call.respondText("Ok", status = HttpStatusCode.OK)
        } catch (e: Exception) {
            println("Error: ${e.message}")
            call.respondText("Error: ${e.message}", status = HttpStatusCode.InternalServerError)
        }
    }
}

fun Route.getPhotoRoute(fileHandler: FileHandler) {
    get("/photo/{filename}") {
        try {
            val fileName = call.parameters["filename"]
            if (fileName == null) {
                call.respondText("Error: Wrong filename", status = HttpStatusCode.BadRequest)
                return@get
            }

            val photoFile = fileHandler.getDevicePhotoNamesFromDisk(fileName)
            if (!photoFile.exists()) {
                call.respondText("Error: file not exists", status = HttpStatusCode.BadRequest)
            }

            call.response.header(
                name = HttpHeaders.ContentDisposition,
                value = ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, fileName).toString()
            )
            call.respondFile(file = photoFile)
        } catch (e: Exception) {
            println("Error: ${e.message}")
            call.respondText("Error: ${e.message}", status = HttpStatusCode.InternalServerError)
        }
    }
}

private fun Route.getPhotosRoute(fileHandler: FileHandler) {
    get("/photos/{deviceId}") {
        try {
            println("HEADERS = ${call.request.headers.flattenEntries()}")
            val deviceId = call.parameters["deviceId"]?.toLongOrNull()
            if (deviceId == null) {
                call.respondText("Error: Wrong deviceId", status = HttpStatusCode.BadRequest)
                return@get
            }

            val from = call.request.queryParameters["from"]?.toLongOrNull()
            val to = call.request.queryParameters["to"]?.toLongOrNull()

            val photos = fileHandler.getDevicePhotosNamesFromDisk(deviceId, from, to).sortedByDescending {  it.dateTime }

            call.respond(message = photos, status = HttpStatusCode.OK)
        } catch (e: Exception) {
            println("Error: ${e.message}")
            call.respondText("Error: ${e.message}", status = HttpStatusCode.InternalServerError)
        }
    }
}
