package com.example.julianczaja

object Configuration {
    var baseUrl = Constants.DEFAULT_BASE_URL
    var port = Constants.DEFAULT_PORT
    var maxSpaceMb = Constants.DEFAULT_MAX_SPACE_MB
    var cleanupIntervalMs = Constants.DEFAULT_CLEANUP_INTERVAL_MS

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
