package com.good4.analytics

import com.good4.core.util.AppEnvironment
import platform.Foundation.NSUserDefaults

/** Mirrors the existing native SDK bridges. Installed before Compose/Koin starts. */
interface NativeAnalytics {
    fun setEnabled(enabled: Boolean)
    fun reset()
    fun log(name: String, parameters: Map<String, String>)
}

object AnalyticsBridge { var native: NativeAnalytics? = null }

class IOSAnalyticsConsentStorage : AnalyticsConsentStorage {
    override fun read(): AnalyticsConsent = NSUserDefaults.standardUserDefaults.stringForKey(ANALYTICS_CONSENT_KEY)
        ?.let { raw -> AnalyticsConsent.entries.firstOrNull { it.name == raw } } ?: AnalyticsConsent.UNKNOWN
    override fun write(value: AnalyticsConsent) {
        NSUserDefaults.standardUserDefaults.setObject(value.name, ANALYTICS_CONSENT_KEY)
    }
}

class IOSAnalyticsTransport : AnalyticsTransport {
    override fun setEnabled(enabled: Boolean) { checkNotNull(AnalyticsBridge.native).setEnabled(enabled) }
    override fun reset() { checkNotNull(AnalyticsBridge.native).reset() }
    override fun log(name: String, parameters: Map<String, String>) { checkNotNull(AnalyticsBridge.native).log(name, parameters) }
}

fun analyticsCollectionAllowed(): Boolean = !AppEnvironment.useFirebaseEmulators &&
    !(AppEnvironment.isDebug && AppEnvironment.firebaseProjectId == "good4tr-v2")
