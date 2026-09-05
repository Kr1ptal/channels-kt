package io.channels.core.blocking

import io.channels.core.ChannelState
import kotlinx.atomicfu.atomic

/** JVM/Android state shared by all blocking waits on one notification handle. */
actual abstract class PlatformNotification protected actual constructor(actual val channelState: ChannelState) {
    // Spin, yield, and sleep waiters never acquire the parking lock.
    private val stateVersion = atomic(0)
    private val parkingLock = PlatformLock()
    private val parkingWaiters = atomic(0)

    protected actual fun signalPlatformStateChange() {
        stateVersion.incrementAndGet()

        // The producer only takes the shared lock when a parking waiter is registered.
        if (parkingWaiters.value > 0) {
            parkingLock.withLock { parkingLock.signalAll() }
        }
    }

    /** Wait with a lock-free busy spin for data, closure, or a state-change signal. */
    fun waitWithBusySpin() {
        val startVersion = stateVersion.value
        while (
            channelState.isEmpty &&
            !channelState.isClosed &&
            stateVersion.value == startVersion
        ) {
            PlatformWaitStrategy.onSpinWait()
        }
    }

    /** Wait by yielding the current thread between state checks. */
    fun waitWithYield() {
        val startVersion = stateVersion.value
        while (
            channelState.isEmpty &&
            !channelState.isClosed &&
            stateVersion.value == startVersion
        ) {
            PlatformWaitStrategy.yieldThread()
        }
    }

    /** Wait on the condition shared by all parking waiters on this handle. */
    fun waitWithParking() {
        val startVersion = stateVersion.value
        if (!channelState.isEmpty || channelState.isClosed) return

        parkingWaiters.incrementAndGet()
        try {
            parkingLock.withLock {
                if (
                    channelState.isEmpty &&
                    !channelState.isClosed &&
                    stateVersion.value == startVersion
                ) {
                    parkingLock.await()
                }
            }
        } finally {
            parkingWaiters.decrementAndGet()
        }
    }

    /** Wait by sleeping for [sleepNanos] between state checks. */
    fun waitWithSleep(sleepNanos: Long) {
        val startVersion = stateVersion.value
        while (
            channelState.isEmpty &&
            !channelState.isClosed &&
            stateVersion.value == startVersion
        ) {
            PlatformWaitStrategy.sleepNanos(sleepNanos)
        }
    }
}
