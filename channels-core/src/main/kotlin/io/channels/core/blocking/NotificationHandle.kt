package io.channels.core.blocking

import io.channels.core.ChannelState
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Highly optimized notification handle for coordinating multiple blocking strategies.
 *
 * Handle is designed to support:
 * - Lock-free fast path for spinning strategies
 * - Consolidated lock for parking strategies to minimize contention
 * - Callback support for custom notification strategies (e.g., coroutines)
 */
class NotificationHandle(val channelState: ChannelState) {
    // Fast path: atomic counter for spinning strategies
    private val stateVersion = AtomicInteger(0)

    // Slow path: consolidated parking for blocking strategies
    private val parkingLock = ReentrantLock()
    private val parkingCondition = parkingLock.newCondition()
    private val parkingWaiters = AtomicInteger(0)

    // Callback support for custom strategies - we use copy-on-write list because there will be
    // many more reads than writes in a normal scenario.
    private val callbacks = CopyOnWriteArrayList<() -> Unit>()

    /**
     * Signal that new data is available - called by sender.
     * Optimized for minimal overhead in the common case.
     */
    fun signalDataAvailable() {
        // Increment version for spinning strategies (lock-free)
        stateVersion.incrementAndGet()

        // Wake up parking strategies only if there are waiters
        if (parkingWaiters.get() > 0) {
            parkingLock.withLock { parkingCondition.signalAll() }
        }

        // Notify any registered callbacks (e.g., for coroutines)
        for (i in callbacks.indices) {
            callbacks[i].invoke()
        }
    }

    /**
     * Register a callback to be invoked when data becomes available.
     * Returns a handle that can be used to unregister the callback.
     */
    fun onDataAvailableCallback(callback: () -> Unit): CallbackHandle {
        callbacks.add(callback)
        return CallbackHandle { callbacks.remove(callback) }
    }

    /**
     * Wait using busy spin strategy - ultra-low latency.
     */
    fun waitWithBusySpin() {
        val startVersion = stateVersion.get()
        while (channelState.isEmpty && stateVersion.get() == startVersion) {
            ThreadHints.onSpinWait()
        }
    }

    /**
     * Wait using yielding strategy - CPU friendly spin.
     */
    fun waitWithYield() {
        val startVersion = stateVersion.get()
        while (channelState.isEmpty && stateVersion.get() == startVersion) {
            Thread.yield()
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
                    parkingCondition.await()
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
        val startVersion = stateVersion.get()
        while (channelState.isEmpty && stateVersion.get() == startVersion) {
            try {
                Thread.sleep(sleepNanos / 1_000_000, (sleepNanos % 1_000_000).toInt())
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                break
            }
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
