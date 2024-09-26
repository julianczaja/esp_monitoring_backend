package com.example.julianczaja.plugins

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.util.*
import io.ktor.util.pipeline.*

fun Application.configureLogging() {
    install(CallLogging) {
        format { call ->
            return@format StringBuilder()
                .appendLine("\n" + " ".repeat(39) + " Request ")
                .append("> ${call.request.httpMethod.value} ")
                .appendLine(call.request.uri)
                .appendLine("> Headers:")
                .append(getHeadersString(call.request.headers))
                .apply {

                    val parameters = call.parameters.flattenEntries()
                    if (parameters.isNotEmpty()) {
                        appendLine("> Parameters: ")
                        parameters.forEach { parameter ->
                            appendLine("\t> ${parameter.first}: '${parameter.second}'")
                        }
                    }
                }
                .appendLine("\n" + " ".repeat(39) + " Response ")
                .appendLine("> Headers:")
                .append(getHeadersString(call.response.headers.allValues()))
                .appendLine("> Status: ${call.response.status()}")
                .appendLine("> Duration: ${call.processingTimeMillis()} ms")
                .append("_".repeat(80))
                .toString()
        }
    }
}

private fun getHeadersString(headers: Headers): String {
    val stringBuilder = StringBuilder()
    headers.flattenEntries().forEach { header ->
        stringBuilder.appendLine("\t> ${header.first}: ${header.second}")
    }
    return stringBuilder.toString()
}

fun enableLogResponseBody(sendPipeline: ApplicationSendPipeline) {
    val phase = PipelinePhase("phase")
    sendPipeline.insertPhaseBefore(ApplicationSendPipeline.Engine, phase)
    sendPipeline.intercept(phase) { response ->
        val responseContent: String = when (response) {
            is OutgoingContent.ByteArrayContent -> String(response.bytes())
            is OutgoingContent.NoContent -> ""
            is OutgoingContent.ReadChannelContent -> "<omitted>"
            is OutgoingContent.WriteChannelContent -> "<omitted>"
            else -> error("")
        }
        println("Response body: $responseContent")
    }
}
