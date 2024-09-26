package com.example.julianczaja.utils

import io.ktor.http.*
import io.ktor.server.response.*


fun ApplicationResponse.addFileNameContentDescriptionHeader(fileName: String) = header(
    name = HttpHeaders.ContentDisposition,
    value = ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, fileName).toString()
)

fun Long.bytesToMegaBytes(): Float = this / (1024f * 1024f)
