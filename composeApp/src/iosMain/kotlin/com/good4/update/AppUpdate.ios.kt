package com.good4.update

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import config.StoreLinks
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import platform.Foundation.NSBundle
import platform.Foundation.NSURL
import platform.Foundation.NSUserDefaults
import platform.UIKit.UIApplication
import kotlin.coroutines.resume

actual fun loadUpdateReminderAt(): Long =
    NSUserDefaults.standardUserDefaults.doubleForKey("update_reminder_at").toLong()
actual fun saveUpdateReminderAt(value: Long) {
    NSUserDefaults.standardUserDefaults.setDouble(value.toDouble(), "update_reminder_at")
}

private suspend fun storefrontCountry(): String? {
    val provider = AppUpdateStorefrontBridge.provider ?: return null
    return withTimeoutOrNull(10_000L) {
        suspendCancellableCoroutine<String?> { continuation ->
            provider.requestCountry(object : NativeUpdateStorefrontCallback {
                override fun onCountry(countryCode: String?) {
                    if (continuation.isActive) {
                        continuation.resume(countryCode?.lowercase()?.takeIf { code ->
                            code.length == 2 && code.all { it in 'a'..'z' }
                        })
                    }
                }
            })
        }
    }
}

private class IosAppUpdateService : AppUpdateService {
    private val client = HttpClient { install(HttpTimeout) { requestTimeoutMillis = 10_000 } }
    private val _status = MutableStateFlow(UpdateStatus.NONE)
    override val status = _status.asStateFlow()
    private var storeUrl: String? = null

    override suspend fun check() {
        // Test bundle IDs must never advertise the production app as an update.
        if (NSBundle.mainBundle.bundleIdentifier != "com.good4.iosApp") return
        val id = Regex("/id([0-9]+)").find(StoreLinks.APP_STORE_URL)?.groupValues?.get(1) ?: return
        val country = storefrontCountry() ?: run {
            storeUrl = null
            _status.update { UpdateStatus.NONE }
            return
        }
        val response = client.get("https://itunes.apple.com/lookup") {
            parameter("id", id)
            parameter("country", country)
        }
        check(response.status.value == 200)
        val item = Json.parseToJsonElement(response.bodyAsText()).jsonObject["results"]
            ?.jsonArray?.firstOrNull()?.jsonObject
        val installed = NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String
        val latest = item?.get("version")?.jsonPrimitive?.content
        val matches = item?.get("bundleId")?.jsonPrimitive?.content == NSBundle.mainBundle.bundleIdentifier &&
            item?.get("trackId")?.jsonPrimitive?.content == id
        val available = matches && installed != null && latest != null && newer(latest, installed)
        storeUrl = if (available) "https://apps.apple.com/$country/app/id$id" else null
        _status.update { if (available) UpdateStatus.AVAILABLE else UpdateStatus.NONE }
    }

    private fun newer(latest: String, installed: String): Boolean {
        val latestParts = latest.split('.').map { it.toIntOrNull() ?: return false }
        val installedParts = installed.split('.').map { it.toIntOrNull() ?: return false }
        for (index in 0 until maxOf(latestParts.size, installedParts.size)) {
            val difference = (latestParts.getOrElse(index) { 0 }).compareTo(installedParts.getOrElse(index) { 0 })
            if (difference != 0) return difference > 0
        }
        return false
    }

    override suspend fun start(): Boolean {
        val url = NSURL.URLWithString(checkNotNull(storeUrl)) ?: error("Invalid store URL")
        return suspendCancellableCoroutine { continuation ->
            UIApplication.sharedApplication.openURL(url, options = emptyMap<Any?, Any>()) { opened ->
                if (continuation.isActive) {
                    if (opened) continuation.resume(true)
                    else continuation.resumeWith(Result.failure(IllegalStateException("Store did not open")))
                }
            }
        }
    }

    override suspend fun complete() { start() }
    fun close() { client.close() }
}

@Composable
actual fun rememberAppUpdateService(): AppUpdateService {
    val service = remember { IosAppUpdateService() }
    DisposableEffect(service) { onDispose { service.close() } }
    return service
}
