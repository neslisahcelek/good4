package com.good4.core.presentation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.seconds

class CooldownTimer(
    private val scope: CoroutineScope,
    private val nowMillisProvider: () -> Long = { Clock.System.now().toEpochMilliseconds() }
) {
    private var cooldownUntilMillis: Long = 0L
    private var cooldownJob: Job? = null

    fun remainingSeconds(): Int {
        val millisLeft = cooldownUntilMillis - nowMillisProvider()
        if (millisLeft <= 0L) return 0
        return (millisLeft / 1000L).coerceAtLeast(1L).toInt()
    }

    fun isActive(): Boolean = remainingSeconds() > 0

    fun start(
        seconds: Int,
        onTick: (remainingSeconds: Int) -> Unit,
        onComplete: () -> Unit
    ) {
        cooldownUntilMillis = nowMillisProvider() + seconds * 1000L
        cooldownJob?.cancel()
        cooldownJob = scope.launch {
            for (remaining in seconds downTo 1) {
                onTick(remaining)
                delay(1.seconds)
            }
            onComplete()
        }
    }

    fun cancel() {
        cooldownJob?.cancel()
    }
}
