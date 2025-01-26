package io.channels.core.blocking

import io.channels.core.ChannelState

/**
 * A [BlockingStrategy] that uses busy-waiting to wait for the next element to become available.
 *
 * This strategy does not park and will use as much CPU cycles as possible, but has the lowest latency jitter. Should
 * be used sparingly to avoid CPU starvation.
 * */
object BusySpinBlockingStrategy : BlockingStrategy {
    override fun waitForStateChange(status: ChannelState) {
        while (status.isEmpty) {
            ThreadHints.onSpinWait()
        }
    }

    override fun signalStateChange() {
        // do nothing - size change is the signal
    }
}
