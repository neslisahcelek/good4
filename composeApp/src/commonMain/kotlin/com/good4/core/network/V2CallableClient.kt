package com.good4.core.network

import com.good4.core.util.AppEnvironment
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

suspend fun callV2Function(name: String, data: JsonObject): JsonObject {
    require(name.matches(Regex("[A-Za-z][A-Za-z0-9_]*"))) { "Invalid callable name" }
    val appCheckToken = if (AppEnvironment.useFirebaseEmulators) null else runCatching { currentAppCheckToken() }.getOrNull()
    val response = callableClient.post(if (AppEnvironment.useFirebaseEmulators) "http://${AppEnvironment.firebaseEmulatorHost}:5105/demo-good4-v2/europe-west1/$name" else v2CallableUrl(AppEnvironment.firebaseProjectId, name)) {
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

internal fun v2CallableUrl(projectId: String, name: String): String {
    require(projectId == "good4tr-v2" || projectId == "good4tr-test") {
        "Unsupported Firebase project: $projectId"
    }
    require(name.matches(Regex("[A-Za-z][A-Za-z0-9_]*"))) { "Invalid callable name" }
    return "https://europe-west1-$projectId.cloudfunctions.net/$name"
}
