package com.good4.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.good4.core.util.AppEnvironment

class AndroidAnalyticsConsentStorage(context: Context) : AnalyticsConsentStorage {
    private val preferences = context.getSharedPreferences("good4_analytics", Context.MODE_PRIVATE)
    override fun read(): AnalyticsConsent = preferences.getString(ANALYTICS_CONSENT_KEY, null)
        ?.let { raw -> AnalyticsConsent.entries.firstOrNull { it.name == raw } } ?: AnalyticsConsent.UNKNOWN
    override fun write(value: AnalyticsConsent) {
        check(preferences.edit().putString(ANALYTICS_CONSENT_KEY, value.name).commit())
    }
}

class AndroidAnalyticsTransport(context: Context) : AnalyticsTransport {
    private val sdk = FirebaseAnalytics.getInstance(context)
    override fun setEnabled(enabled: Boolean) {
        sdk.setConsent(mapOf(
            FirebaseAnalytics.ConsentType.ANALYTICS_STORAGE to if (enabled) FirebaseAnalytics.ConsentStatus.GRANTED else FirebaseAnalytics.ConsentStatus.DENIED,
            FirebaseAnalytics.ConsentType.AD_STORAGE to FirebaseAnalytics.ConsentStatus.DENIED,
            FirebaseAnalytics.ConsentType.AD_USER_DATA to FirebaseAnalytics.ConsentStatus.DENIED,
            FirebaseAnalytics.ConsentType.AD_PERSONALIZATION to FirebaseAnalytics.ConsentStatus.DENIED
        ))
        sdk.setAnalyticsCollectionEnabled(enabled)
    }
    override fun reset() { sdk.resetAnalyticsData(); setEnabled(false) }
    override fun log(name: String, parameters: Map<String, String>) {
        sdk.logEvent(name, Bundle().apply { parameters.forEach { (key, value) -> putString(key, value) } })
    }
}

fun analyticsCollectionAllowed(): Boolean = !AppEnvironment.useFirebaseEmulators &&
    !(AppEnvironment.isDebug && AppEnvironment.firebaseProjectId == "good4tr-v2")
