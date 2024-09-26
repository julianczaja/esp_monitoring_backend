package com.example.julianczaja

import com.example.julianczaja.utils.DEFAULT_BASE_URL
import com.example.julianczaja.utils.DEFAULT_CLEANUP_INTERVAL_MS
import com.example.julianczaja.utils.DEFAULT_MAX_SPACE_MB
import com.example.julianczaja.utils.DEFAULT_PORT


object Configuration {
    var baseUrl = DEFAULT_BASE_URL
    var port = DEFAULT_PORT
    var maxSpaceMb = DEFAULT_MAX_SPACE_MB
    var cleanupIntervalMs = DEFAULT_CLEANUP_INTERVAL_MS

    val fullUrl
        get() = "http://$baseUrl:$port"

    override fun toString(): String {
        return StringBuilder()
            .appendLine("Base URL: $baseUrl")
            .appendLine("Port: $port")
            .appendLine("Max space: $maxSpaceMb MB")
            .append("Cleanup interval: $cleanupIntervalMs ms")
            .toString()
    }
}
