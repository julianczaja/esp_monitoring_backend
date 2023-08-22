package com.example.julianczaja

import com.example.julianczaja.Constants.BASE_URL
import com.example.julianczaja.Constants.PORT
import com.example.julianczaja.plugins.configureMonitoring
import com.example.julianczaja.plugins.configureRouting
import com.example.julianczaja.plugins.configureSerialization
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
    embeddedServer(
        Netty,
        host = BASE_URL,
        port = PORT,
        module = Application::module
    ).start(wait = true)
}

fun Application.module() {
    val fileHandler = FileHandler()

    configureSerialization()
    configureMonitoring()
    configureRouting(fileHandler)
}
