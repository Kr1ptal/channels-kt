package io.channels.core.waiting

import io.channels.core.ChannelState

/**
 * A [WaitStrategy] that uses [Thread.yield] to wait for the next element to become available.
 *
 * This strategy does not park and will use as much CPU cycles as possible, but can yield the CPU to other threads if
 * needed.
 * */
object YieldingWaitStrategy : WaitStrategy {
    override fun waitForNextElement(status: ChannelState) {
        while (status.isEmpty) {
            Thread.yield()
        }
    }

    override fun signalStateChange() {
        // do nothing - size change is the signal
    }
}