package io.channels.core.waiting

import io.channels.core.ChannelState

/**
 * A [WaitStrategy] that uses busy-waiting to wait for the next element to become available.
 *
 * This strategy does not park and will use as much CPU cycles as possible, but has the lowest latency jitter. Should
 * be used sparingly to avoid CPU starvation.
 * */
object BusySpinWaitStrategy : WaitStrategy {
    override fun waitForNextElement(status: ChannelState) {
        while (status.isEmpty) {
            ThreadHints.onSpinWait()
        }
    }

    override fun signalStateChange() {
        // do nothing - size change is the signal
    }
}