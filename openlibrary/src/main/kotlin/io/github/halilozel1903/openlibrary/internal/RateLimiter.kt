package io.github.halilozel1903.openlibrary.internal

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Spaces request starts at least `1 / perSecond` apart. Callers reserve a slot under a lock and wait outside it, so
 * many coroutines queue in order without holding the lock while they sleep.
 */
internal class RateLimiter(
    perSecond: Double,
    private val nowNanos: () -> Long = System::nanoTime,
) {
    private val intervalNanos: Long = if (perSecond > 0) (1_000_000_000.0 / perSecond).toLong() else 0L
    private val mutex = Mutex()
    private var nextSlot = Long.MIN_VALUE

    /** How long the caller has to wait for its slot, in milliseconds. Reserves the slot. */
    suspend fun reserve(): Long {
        if (intervalNanos == 0L) return 0
        return mutex.withLock {
            val now = nowNanos()
            val slot = if (nextSlot == Long.MIN_VALUE || nextSlot < now) now else nextSlot
            nextSlot = slot + intervalNanos
            (slot - now) / 1_000_000
        }
    }

    suspend fun acquire() {
        val wait = reserve()
        if (wait > 0) delay(wait)
    }
}
