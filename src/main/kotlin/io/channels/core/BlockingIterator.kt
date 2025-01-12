package io.channels.core

import io.channels.core.waiting.WaitStrategy

/**
 * An iterator that blocks using [strategy] until the [receiver] has a next element or is closed.
 * */
class BlockingIterator<T: Any>(
    private val receiver: ChannelReceiver<T>,
    private val strategy: WaitStrategy,
    private val isClosed: () -> Boolean,
) : Iterator<T> {
    private var next: T? = null

    fun signalStateChange() {
        strategy.signalStateChange()
    }

    override fun hasNext(): Boolean {
        if (this.next != null) {
            return true
        }

        var next: T? = null
        while (next == null) {
            next = receiver.tryPoll()

            // check after polling, so we still drain the queue even if unsubscribed
            if (isClosed()) {
                break
            }

            // if no next element, wait until next event to avoid CPU cycle burning
            if (next == null) {
                strategy.waitForNextElement(receiver)
            }
        }

        this.next = next
        return next != null
    }

    override fun next(): T {
        val ret = next ?: throw NoSuchElementException("Need to call hasNext() before calling next()")
        next = null
        return ret
    }
}
