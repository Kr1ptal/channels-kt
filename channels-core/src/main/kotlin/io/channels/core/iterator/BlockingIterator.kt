package io.channels.core.iterator

import io.channels.core.ChannelReceiver
import io.channels.core.blocking.NotificationHandle

/**
 * An iterator that blocks using [waitFunction] until the [receiver] has a next element or is closed.
 * */
class BlockingIterator<T : Any>(
    private val receiver: ChannelReceiver<T>,
    private val waitFunction: (NotificationHandle) -> Unit,
    private val isClosed: () -> Boolean,
) : Iterator<T> {
    private var next: T? = null
    private val notificationHandle = NotificationHandle(receiver)

    override fun hasNext(): Boolean {
        if (this.next != null) {
            return true
        }

        var next: T? = null
        while (next == null) {
            next = receiver.poll()

            // check after polling, so we still drain the queue even if unsubscribed
            if (isClosed()) {
                break
            }

            // if no next element, wait until next event to avoid CPU cycle burning
            if (next == null) {
                waitFunction(notificationHandle)
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
