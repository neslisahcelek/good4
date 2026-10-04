package com.good4.core.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

private val callableJson = Json { ignoreUnknownKeys = true }

private val callableClient = HttpClient {
    install(ContentNegotiation) { json(callableJson) }
    install(HttpTimeout) {
        requestTimeoutMillis = 30_000
        connectTimeoutMillis = 10_000
        socketTimeoutMillis = 30_000
    }
}

expect suspend fun currentFirebaseIdToken(): String
expect suspend fun currentAppCheckToken(): String?

object V2Functions {
    /** Debug builds point this at the local Functions emulator (see tools/landing-demo/README.md). */
    var baseUrl: String = "https://europe-west1-good4tr-v2.cloudfunctions.net"
}

suspend fun callV2Function(name: String, data: JsonObject): JsonObject {
    val appCheckToken = runCatching { currentAppCheckToken() }.getOrNull()
    val response = callableClient.post("${V2Functions.baseUrl}/$name") {
        contentType(ContentType.Application.Json)
        bearerAuth(currentFirebaseIdToken())
        if (!appCheckToken.isNullOrBlank()) {
            header("X-Firebase-AppCheck", appCheckToken)
        }
        setBody(buildJsonObject { put("data", data) })
    }.body<JsonObject>()
    response["error"]?.jsonObject?.let { error ->
        error(error["message"]?.jsonPrimitive?.content ?: "İşlem tamamlanamadı.")
    }
    return (response["data"] ?: response["result"])?.jsonObject ?: JsonObject(emptyMap())
}
