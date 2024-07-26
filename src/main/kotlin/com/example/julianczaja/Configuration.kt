package com.example.julianczaja

object Configuration {
    var baseUrl = Constants.DEFAULT_BASE_URL
    var port = Constants.DEFAULT_PORT
    var maxSpaceMb = Constants.DEFAULT_MAX_SPACE_MB

    val fullUrl
        get() = "http://$baseUrl:$port"

    override fun toString(): String {
        return StringBuilder()
            .appendLine("Base URL: $baseUrl")
            .appendLine("Port: $port")
            .append("Max space: $maxSpaceMb MB")
            .toString()
    }
}
