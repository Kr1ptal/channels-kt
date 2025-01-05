package io.channels.core.waiting

import io.channels.core.ChannelState
import java.time.Duration
import java.util.concurrent.locks.LockSupport

/**
 * A [WaitStrategy] that uses [LockSupport.parkNanos] to wait for the next element to become available.
 *
 * The [sleepDuration] is used to determine how long to wait. A higher number will lead to higher latency, but use
 * less CPU cycles, and vice versa. This strategy can provide a good balance between latency and CPU usage.
 * */
class SleepingWaitStrategy(sleepDuration: Duration) : WaitStrategy {
    private val sleepDurationNanos = sleepDuration.toNanos()

    constructor() : this(Duration.ofNanos(100))

    override fun waitForNextElement(status: ChannelState) {
        while (status.isEmpty) {
            LockSupport.parkNanos(sleepDurationNanos);
        }
    }

    override fun signalStateChange() {
        // do nothing - size change is the signal
    }
}