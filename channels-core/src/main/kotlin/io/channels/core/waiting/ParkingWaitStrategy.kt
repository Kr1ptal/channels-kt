package io.channels.core.waiting

import io.channels.core.ChannelState
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * A [WaitStrategy] that parks the current thread until signaled the next element is available.
 *
 * Uses the fewest CPU cycles, but will lead to higher latency jitter if parked.
 * */
class ParkingWaitStrategy : WaitStrategy {
    private val lock = ReentrantLock()
    private val condition = lock.newCondition()

    override fun waitForNextElement(status: ChannelState) {
        lock.withLock {
            // if the queue is not empty, we don't need to wait
            if (!status.isEmpty) return

            condition.await()
        }
    }

    override fun signalStateChange() {
        lock.withLock { condition.signal() }
    }
}
