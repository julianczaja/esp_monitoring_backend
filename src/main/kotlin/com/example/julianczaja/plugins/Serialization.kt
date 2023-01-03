package com.example.julianczaja.plugins

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json()
//        register(ContentType.Image.JPEG, JpegSerializationConverter(BinaryFormat()))
    }
}
