package io.channels.core.blocking

import io.channels.core.ChannelState

/**
 * A [BlockingStrategy] that uses [Thread.yield] to wait for the next element to become available.
 *
 * This strategy does not park and will use as much CPU cycles as possible, but can yield the CPU to other threads if
 * needed.
 * */
object YieldingBlockingStrategy : BlockingStrategy {
    override fun waitForStateChange(status: ChannelState) {
        while (status.isEmpty) {
            Thread.yield()
        }
    }

    override fun signalStateChange() {
        // do nothing - size change is the signal
    }
}
