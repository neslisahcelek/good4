package com.good4.core.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
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
}

expect suspend fun currentFirebaseIdToken(): String

suspend fun callV2Function(name: String, data: JsonObject): JsonObject {
    val response = callableClient.post("https://europe-west1-good4tr-v2.cloudfunctions.net/$name") {
        contentType(ContentType.Application.Json)
        bearerAuth(currentFirebaseIdToken())
        setBody(buildJsonObject { put("data", data) })
    }.body<JsonObject>()
    response["error"]?.jsonObject?.let { error ->
        error(error["message"]?.jsonPrimitive?.content ?: "İşlem tamamlanamadı.")
    }
    return (response["data"] ?: response["result"])?.jsonObject ?: JsonObject(emptyMap())
}
