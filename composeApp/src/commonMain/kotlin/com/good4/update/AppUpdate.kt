package com.good4.update

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow

enum class UpdateStatus { NONE, AVAILABLE, DOWNLOADING, READY, FAILED }

/** Store integration only; prompting and reminder policy belong to the ViewModel. */
interface AppUpdateService {
    val status: StateFlow<UpdateStatus>
    /** Store consent results may arrive after the original caller has been destroyed. */
    val declinedUpdates: Flow<Unit> get() = emptyFlow()
    /** True when start() only opens the store and the app cannot tell whether the update was installed. */
    val snoozeAfterStart: Boolean get() = false
    suspend fun check()
    /** False means the user declined or the update is no longer available. */
    suspend fun start(): Boolean
    suspend fun complete()
}

@Composable
expect fun rememberAppUpdateService(): AppUpdateService
expect fun loadUpdateReminderAt(): Long
expect fun saveUpdateReminderAt(value: Long)

/** Swift obtains the account storefront asynchronously and returns an ISO alpha-2 code. */
interface NativeUpdateStorefrontCallback {
    fun onCountry(countryCode: String?)
}

interface NativeUpdateStorefrontProvider {
    fun requestCountry(callback: NativeUpdateStorefrontCallback)
}

object AppUpdateStorefrontBridge {
    var provider: NativeUpdateStorefrontProvider? = null
}
