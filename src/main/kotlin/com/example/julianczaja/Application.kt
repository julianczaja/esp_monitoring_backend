package com.example.julianczaja

import com.example.julianczaja.Constants.DEFAULT_BASE_URL
import com.example.julianczaja.Constants.DEFAULT_CLEANUP_INTERVAL_MS
import com.example.julianczaja.Constants.DEFAULT_MAX_SPACE_MB
import com.example.julianczaja.Constants.DEFAULT_PORT
import com.example.julianczaja.plugins.configureLogging
import com.example.julianczaja.plugins.configureRouting
import com.example.julianczaja.plugins.configureSerialization
import com.example.julianczaja.plugins.configureTemplating
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default

fun main(args: Array<String>) {
    parseArgs(args)
    embeddedServer(
        Netty,
        host = Configuration.baseUrl,
        port = Configuration.port,
        module = Application::module
    ).start(wait = true)
}

fun Application.module() {
    log.info("Starting with configuration:\n$Configuration")

    val fileHandler = FileHandler()
//    fileHandler.movePhotosToCorrectDirs()
//    fileHandler.createMissingThumbnails()
//    enableLogResponseBody(sendPipeline)

    configureSerialization()
    configureLogging()
    configureTemplating()
    configureRouting(fileHandler)
}

private fun parseArgs(args: Array<String>) {
    val parser = ArgParser("esp-monitoring-backend")

    val baseUrl by parser.option(
        type = ArgType.String,
        shortName = "url",
        description = "Base URL"
    ).default(DEFAULT_BASE_URL)

    val port by parser.option(
        type = ArgType.Int,
        shortName = "p",
        description = "Port number"
    ).default(DEFAULT_PORT)

    val maxSpaceMb by parser.option(
        type = ArgType.Int,
        shortName = "s",
        description = "Max photos space per device (MB)"
    ).default(DEFAULT_MAX_SPACE_MB)

    val cleanupIntervalMs by parser.option(
        type = ArgType.Int,
        shortName = "c",
        description = "Photos cleanup interval (ms)"
    ).default(DEFAULT_CLEANUP_INTERVAL_MS)

    parser.parse(args)

    Configuration.baseUrl = baseUrl
    Configuration.port = port
    Configuration.maxSpaceMb = maxSpaceMb
    Configuration.cleanupIntervalMs = cleanupIntervalMs
}
