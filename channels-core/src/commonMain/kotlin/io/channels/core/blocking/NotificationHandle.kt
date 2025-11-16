package io.channels.core.blocking

import io.channels.core.ChannelState
import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update

/**
 * Highly optimized notification handle for coordinating multiple blocking strategies.
 *
 * Handle is designed to support:
 * - Lock-free fast-path for spinning strategies
 * - Consolidated lock for parking strategies to minimize contention
 * - Callback support for custom notification strategies (e.g., coroutines)
 */
class NotificationHandle(val channelState: ChannelState) {
    // Fast path: atomic counter for spinning strategies
    private val stateVersion = atomic(0)

    // Slow path: consolidated parking for blocking strategies
    private val parkingLock = PlatformLock()
    private val parkingWaiters = atomic(0)

    // Callback support for custom strategies - we use atomic reference with an immutable list because
    // there will be many more reads than writes in a normal scenario.
    private val callbacks: AtomicRef<List<() -> Unit>> = atomic(emptyList())

    /**
     * Signal that channel state has changed - called by sender. Optimized for minimal overhead
     * in the common case.
     */
    fun signalStateChange() {
        // Increment version for spinning strategies (lock-free)
        stateVersion.incrementAndGet()

        // Wake up parking strategies only if there are waiters
        if (parkingWaiters.value > 0) {
            parkingLock.withLock { parkingLock.signalAll() }
        }

        // Notify any registered callbacks (e.g., for coroutines)
        for (callback in callbacks.value) {
            callback.invoke()
        }
    }

    /**
     * Register a callback to be invoked when channel state changes. Returns a handle that can be
     * used to unregister the callback.
     */
    fun onStateChangeCallback(callback: () -> Unit): CallbackHandle {
        callbacks.update { it + callback }
        return CallbackHandle { callbacks.update { list -> list - callback } }
    }

    /**
     * Wait using busy spin strategy - ultra-low latency, but very high CPU usage.
     */
    fun waitWithBusySpin() {
        val startVersion = stateVersion.value
        while (channelState.isEmpty && stateVersion.value == startVersion) {
            PlatformWaitStrategy.onSpinWait()
        }
    }

    /**
     * Wait using yielding strategy - CPU friendly spin.
     */
    fun waitWithYield() {
        val startVersion = stateVersion.value
        while (channelState.isEmpty && stateVersion.value == startVersion) {
            PlatformWaitStrategy.yieldThread()
        }
    }

    /**
     * Wait using parking strategy - most CPU efficient.
     */
    fun waitWithParking() {
        // Fast check before expensive parking
        if (!channelState.isEmpty) return

        parkingWaiters.incrementAndGet()
        try {
            parkingLock.withLock {
                // Double-check inside lock to avoid lost wake-ups
                if (channelState.isEmpty) {
                    parkingLock.await()
                }
            }
        } finally {
            parkingWaiters.decrementAndGet()
        }
    }

    /**
     * Wait using sleep strategy with specified duration.
     */
    fun waitWithSleep(sleepNanos: Long) {
        val startVersion = stateVersion.value
        while (channelState.isEmpty && stateVersion.value == startVersion) {
            PlatformWaitStrategy.sleepNanos(sleepNanos)
        }
    }

    /**
     * Handle for managing callback registration/deregistration.
     */
    fun interface CallbackHandle {
        /**
         * Unregister the callback.
         */
        fun unregister()
    }
}
