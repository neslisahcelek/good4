package com.good4.update

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.good4.BuildConfig
import com.google.android.gms.tasks.Task
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private fun preferences() = GlobalContext.get().get<Context>()
    .getSharedPreferences("good4_update", Context.MODE_PRIVATE)
// Debug reminders last for the process, so a cold launch starts a fresh test.
// Simulation never reads or changes the real store reminder history.
private var debugReminderAt = 0L
actual fun loadUpdateReminderAt(): Long = if (BuildConfig.DEBUG && BuildConfig.SIMULATE_APP_UPDATE)
    debugReminderAt else preferences().getLong("reminder_at", 0L)
actual fun saveUpdateReminderAt(value: Long) {
    if (BuildConfig.DEBUG && BuildConfig.SIMULATE_APP_UPDATE) debugReminderAt = value
    else preferences().edit().putLong("reminder_at", value).apply()
}

private class DebugAppUpdateService(private val scope: CoroutineScope) : AppUpdateService {
    private val _status = MutableStateFlow(UpdateStatus.AVAILABLE)
    override val status = _status.asStateFlow()
    private var download: Job? = null

    override suspend fun check() = Unit

    override suspend fun start(): Boolean {
        if (_status.value != UpdateStatus.AVAILABLE) return false
        _status.update { UpdateStatus.DOWNLOADING }
        download = scope.launch {
            delay(3_000L)
            _status.update { UpdateStatus.READY }
        }
        return true
    }

    override suspend fun complete() {
        // Simulate successful installation without terminating the app or changing its version.
        _status.update { UpdateStatus.NONE }
    }

    fun close() { download?.cancel() }
}

private fun Context.activity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.activity()
    else -> null
}

private suspend fun <T> Task<T>.awaitUpdate(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { if (continuation.isActive) continuation.resume(it) }
    addOnFailureListener { if (continuation.isActive) continuation.resumeWithException(it) }
    addOnCanceledListener { continuation.cancel() }
}

private class AndroidAppUpdateService(private val context: Context) : AppUpdateService {
    private val manager = AppUpdateManagerFactory.create(context)
    private val _status = MutableStateFlow(UpdateStatus.NONE)
    override val status = _status.asStateFlow()
    // Buffer restored results until the home ViewModel attaches; store checks cannot overwrite them.
    private val declines = Channel<Unit>(Channel.BUFFERED)
    override val declinedUpdates = declines.receiveAsFlow()
    var launcher: androidx.activity.result.ActivityResultLauncher<IntentSenderRequest>? = null
    private var pending: CancellableContinuation<Boolean>? = null
    private val enabled = BuildConfig.APPLICATION_ID == "com.good4"
    private val listener = InstallStateUpdatedListener { state ->
        when (state.installStatus()) {
            InstallStatus.DOWNLOADING, InstallStatus.PENDING -> _status.update { UpdateStatus.DOWNLOADING }
            InstallStatus.DOWNLOADED -> _status.update { UpdateStatus.READY }
            InstallStatus.FAILED -> _status.update { UpdateStatus.FAILED }
            InstallStatus.CANCELED -> _status.update { UpdateStatus.AVAILABLE }
            InstallStatus.INSTALLED -> _status.update { UpdateStatus.NONE }
        }
    }

    fun observe() { if (enabled) manager.registerListener(listener) }
    fun close() {
        if (enabled) manager.unregisterListener(listener)
        pending?.cancel()
        pending = null
        launcher = null
    }

    fun onResult(resultCode: Int) {
        val continuation = pending
        pending = null
        // The launcher can restore a result after process death, when no coroutine survives.
        // Publish the result regardless; only resuming the original caller is conditional.
        when (resultCode) {
            Activity.RESULT_OK -> {
                _status.update { UpdateStatus.DOWNLOADING }
                if (continuation?.isActive == true) continuation.resume(true)
            }
            Activity.RESULT_CANCELED -> {
                declines.trySend(Unit)
                _status.update { UpdateStatus.AVAILABLE }
                if (continuation?.isActive == true) continuation.resume(false)
            }
            else -> {
                _status.update { UpdateStatus.FAILED }
                if (continuation?.isActive == true) {
                    continuation.resumeWithException(IllegalStateException("Update flow failed"))
                }
            }
        }
    }

    override suspend fun check() {
        if (!enabled) return
        val info = manager.appUpdateInfo.awaitUpdate()
        _status.update {
            when (info.installStatus()) {
                InstallStatus.DOWNLOADED -> UpdateStatus.READY
                InstallStatus.DOWNLOADING, InstallStatus.PENDING -> UpdateStatus.DOWNLOADING
                else -> if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                    info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) UpdateStatus.AVAILABLE else UpdateStatus.NONE
            }
        }
    }

    override suspend fun start(): Boolean {
        val activity = context.activity() ?: error("No update Activity")
        check(!activity.isFinishing && !activity.isDestroyed)
        val info = manager.appUpdateInfo.awaitUpdate()
        if (info.installStatus() == InstallStatus.DOWNLOADED) {
            _status.update { UpdateStatus.READY }
            return true
        }
        if (info.updateAvailability() != UpdateAvailability.UPDATE_AVAILABLE ||
            !info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
            check()
            return false
        }
        return suspendCancellableCoroutine { continuation ->
            pending = continuation
            continuation.invokeOnCancellation { if (pending === continuation) pending = null }
            try {
                val started = manager.startUpdateFlowForResult(info, checkNotNull(launcher),
                    AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build())
                if (!started && continuation.isActive) {
                    pending = null
                    continuation.resumeWithException(IllegalStateException("Update did not start"))
                }
            } catch (error: Exception) {
                pending = null
                if (continuation.isActive) continuation.resumeWithException(error)
            }
        }
    }

    override suspend fun complete() { manager.completeUpdate().awaitUpdate() }
}

@Composable
actual fun rememberAppUpdateService(): AppUpdateService {
    if (BuildConfig.DEBUG && BuildConfig.SIMULATE_APP_UPDATE) {
        val scope = rememberCoroutineScope()
        val service = remember(scope) { DebugAppUpdateService(scope) }
        DisposableEffect(service) { onDispose { service.close() } }
        return service
    }
    val context = LocalContext.current
    val service = remember(context) { AndroidAppUpdateService(context) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        service.onResult(it.resultCode)
    }
    DisposableEffect(service, launcher) {
        service.launcher = launcher
        service.observe()
        onDispose { service.close() }
    }
    return service
}
