package io.channels.core.waiting

import io.channels.core.ChannelState

/**
 * A strategy that waits for the next element to become available.
 * */
interface WaitStrategy {
    /**
     * Wait for the next element to become available.
     * */
    fun waitForNextElement(status: ChannelState)

    /**
     * Signal that the state has changed and that [waitForNextElement] can return.
     * */
    fun signalStateChange()
}
