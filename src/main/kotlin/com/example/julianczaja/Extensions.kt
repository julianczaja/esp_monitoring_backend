package com.example.julianczaja

import io.ktor.http.*
import io.ktor.server.response.*


fun ApplicationResponse.addFileNameContentDescriptionHeader(fileName: String) = header(
    name = HttpHeaders.ContentDisposition,
    value = ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, fileName).toString()
)
