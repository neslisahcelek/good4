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

/** Set by the debug iOS host when it points Firebase at the local emulators. */
object FirebaseEmulator {
    var host: String? = null
}

expect suspend fun currentFirebaseIdToken(): String
expect suspend fun currentAppCheckToken(): String?

suspend fun callV2Function(name: String, data: JsonObject): JsonObject {
    val appCheckToken = runCatching { currentAppCheckToken() }.getOrNull()
    val url = FirebaseEmulator.host?.let { "http://$it:5105/good4tr-v2/europe-west1/$name" }
        ?: "https://europe-west1-good4tr-v2.cloudfunctions.net/$name"
    val response = callableClient.post(url) {
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
