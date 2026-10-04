package config

import com.good4.core.util.AppEnvironment
import com.good4.core.util.FirebaseBackend

/**
 * Features that are not wired to the V2 backend yet and therefore stay hidden
 * in both production and test V2 builds. Flip once ready.
 */
object ReleaseFeatures {
    private val isV2 get() = AppEnvironment.firebaseBackend == FirebaseBackend.V2

    /**
     * Askıda Yemek: V1 builds use products/codes, V2 builds the campaign screen (SuspendedMealsScreen).
     * Hidden for this release: edu verification codes need the mail extension, which is not set up yet.
     */
    val suspendedMeals: Boolean get() = false

    /** Edu mail + password sign-in and sign-up on the V2 login screen; hidden, Google/Apple only. */
    val eduEmailAuth: Boolean get() = AppEnvironment.useFirebaseEmulators

    /** Kampüs Dolabı runs only on the V2 backend (market callables in good4tr-v2). */
    val campusCloset: Boolean get() = isV2

    /** The in-app business and admin panels use V1 collections; V2 staff use the web panel. */
    val inAppStaffPanels: Boolean get() = !isV2

    val WEB_PANEL_URL: String get() = if (AppEnvironment.useFirebaseEmulators) "http://${AppEnvironment.firebaseEmulatorHost}:5005/admin" else "https://${AppEnvironment.firebaseProjectId}.web.app/admin"
}
