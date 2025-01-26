package io.channels.core.blocking

import io.channels.core.ChannelState

/**
 * A strategy that waits for the next element to become available.
 * */
interface BlockingStrategy {
    /**
     * Wait for the state change to be signaled.
     * */
    fun waitForStateChange(status: ChannelState)

    /**
     * Signal that the state has changed and that [waitForStateChange] can return.
     * */
    fun signalStateChange()
}
