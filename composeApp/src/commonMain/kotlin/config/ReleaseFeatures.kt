package config

import com.good4.core.util.AppEnvironment
import com.good4.core.util.FirebaseBackend

/**
 * Central release switches for flows whose availability is not determined by
 * backend compatibility alone. Production and test apps currently both use V2.
 */
object ReleaseFeatures {
    private val isV2 get() = AppEnvironment.firebaseBackend == FirebaseBackend.V2

    /**
     * The V2 campaign screen exists, but the entry point remains hidden until
     * edu verification mail delivery is available. This is disabled on every backend.
     */
    val suspendedMeals: Boolean get() = false

    /** Local email/password login and registration UI; this does not control edu verification mail delivery. */
    val eduEmailAuth: Boolean get() = AppEnvironment.useFirebaseEmulators

    /** Kampüs Dolabı uses V2 market callables and is available on V2 projects. */
    val campusCloset: Boolean get() = isV2

    /** Sosyal etkinlikler use the V2 social callables; the server keeps them off until app_config enables them. */
    val socialActivities: Boolean get() = isV2

    /** In-app staff panels still use legacy collections; V2 staff are sent to the web panel. */
    val inAppStaffPanels: Boolean get() = !isV2

    val WEB_PANEL_URL: String get() = if (AppEnvironment.useFirebaseEmulators) "http://${AppEnvironment.firebaseEmulatorHost}:5005/admin" else "https://${AppEnvironment.firebaseProjectId}.web.app/admin"
}
